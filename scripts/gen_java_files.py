import sys
import json
from datetime import datetime

def info(msg):
    print ("INFO [{0}]: {1}".format(str(datetime.now()), msg))


def debug(msg):
    print ("DEBUG [{0}]: {1}".format(str(datetime.now()), msg))


def error(msg):
    print ("ERROR [{0}]: {1}".format(str(datetime.now()), msg))


def get_json(file):
    with open(cfg_file) as f:
        return json.load(f)


primitive_types = { "char": 1, "int": 1, "long": 1 }

primitive_type_2_get = { "char": "getChar",
                        "int": "getInt",
                        "long": "getLong" }

primitive_type_2_put = { "char": "putChar",
                        "int": "putInt",
                        "long": "putLong" }

type_to_size = { "char": "Constants.SIZE_OF_CHAR",
                "int": "Constants.SIZE_OF_INT",
                "long": "Constants.SIZE_OF_LONG",
                "string": "Constants.SIZE_OF_LONG"}


def gen_imports(properties, pkg, file):
    # Standard imports
    
    file.write("import sk.mmap.Utils;\n")
    file.write("import sk.mmap.Constants;\n")
    file.write("import sk.mmap.IUnsafeAllocator;\n")
    
    # Import Java and sk classes based on type of properties
    for p in properties:
        if isinstance(p['type'], dict):
            typ = p['type']['type']
            of = p['type']['of']
            #if typ == "list" and of == 'long':
            #    file.write("import sk.lang.MIUString;\n")
            #else:
            #    error("Complex type {0} of {1} is not supported".format(typ, of))
            if typ == "hashmap":
                file.write("\nimport {0}.{1};".format(pkg, get_hashmap_name(of)))
        elif p['type'] in primitive_types:
            pass
        elif p['type'] == 'string':
            file.write("\nimport sk.lang.MIUString;")
        else:
            error("Type {0} is not supported".format(p['type']))

    file.write('\n')


def gen_variable_size_offset(name, size, prev, file):
    file.write("\n    private static int {0}_SIZE = {1};\n".format(name, size))
    if not prev:
        file.write("    private static int {0}_OFFSET = 0;\n".format(name))
    else:
        file.write("    private static int {0}_OFFSET = {1}_OFFSET + {1}_SIZE;\n".format(name, prev))


def gen_variables(properties, file):
    prev = None
    for p in properties:
        name = p['name']
        if isinstance(p['type'], dict):
            typ = p['type']['type']
            of = p['type']['of']
            #error("Complex type {0} of {1} is not supported".format(typ, of))
            # Store pointer to all complex types (non-primitive types) 
            gen_variable_size_offset(name, "Constants.SIZE_OF_LONG", prev, file)
        elif p['type'] in type_to_size:
            gen_variable_size_offset(name, type_to_size[p['type']], prev, file)
        else:
            error("Type {0} is not supported".format(p['type']))
        prev = name


def gen_sizeof(properties, file):
    file.write("\n    private static int sizeof() {\n");
    file.write("        return")
    first = True
    for p in properties:
        name = p['name']
        if not first:
            file.write(" +")
        file.write(" {0}_SIZE".format(name))
        first = False
    file.write(";\n")
    file.write("    }\n")


def gen_create(properties, file):
    file.write("\n    public static long create\n");
    file.write("        (final IUnsafeAllocator allocator");
    
    for p in properties:
        if isinstance(p['type'], dict):
            file.write(",\n        final long {0}".format(p['name']))
        elif p['type'] in primitive_types:
            file.write(",\n        final {0} {1}".format(p['type'], p['name']))
        elif p['type'] == 'string':
            file.write(",\n        final long {0}".format(p['name']))
        else:
            error("Type {0} is not supported".format(p['type']))
            
    file.write(") {\n")
    
    file.write("        final long handle = allocator.alloc(sizeof());\n")
    file.write("        final int bufferOffset = Utils.getBufferIndex(handle);\n")
    
    file.write("        allocator.getByteBuffer(handle)")
    for p in properties:
        name = p['name']
        typ = p['type']
        if isinstance(typ, dict):
            file.write("\n            .putLong(bufferOffset + {0}_OFFSET, {0})".format(name))
        elif typ in primitive_types:
            file.write("\n            .{0}(bufferOffset + {1}_OFFSET, {1})".format(primitive_type_2_put[typ], name))
        elif typ == 'string':
            file.write("\n            .putLong(bufferOffset + {0}_OFFSET, {0})".format(name))
        else:
            error("Type {0} is not supported".format(typ))
    
    file.write(";\n        return handle;\n    }\n")


def gen_getkey(obj, file):
    if "id" in obj:
        key = obj['id']
        idType = None
        for p in obj['properties']:
            if key == p['name']:
                idType = p['type']
                # break;
        if idType:
            if idType == 'long':
                file.write("\n    public static long getKey\n")
                file.write("        (final IUnsafeAllocator allocator,\n")
                file.write("         final long handle) {\n")
                file.write("        return allocator.getByteBuffer(handle)\n")
                file.write("            .getLong(Utils.getBufferIndex(handle) + {0}_OFFSET);\n".format(key))
                file.write("    }\n")
            else:
                error("id {0} of class {1} is of type {2} in properties section, but only long is supported".format(key, obj['class'], idType))
        else:
            error("id {0} of class {1} has not type in properties section".format(key, obj['class']))


def gen_foreach():
    pass


def gen_getters(clasz, props, file):
    for p in props:
        name = p['name']
        typ = p['type']
        if isinstance(typ, dict) or typ == "string":
            file.write("\n    public static long get_{0}".format(name))
            file.write("\n        (final IUnsafeAllocator allocator,")
            file.write("\n        final long handle) {")
            file.write("\n        if (handle == Constants.NULL) {")
            file.write("\n            throw new NullPointerException(\"{0} handle is NULL in get_{1}\");".format(clasz, name))
            file.write("\n        }\n")
            file.write("\n        return allocator.getByteBuffer(handle)")
            file.write("\n            .getLong(Utils.getBufferIndex(handle) + {0}_OFFSET);".format(name))
            file.write("\n    }\n")
        elif typ in primitive_types:
            file.write("\n    public static {0} get_{1}".format(typ, name))
            file.write("\n        (final IUnsafeAllocator allocator,")
            file.write("\n        final long handle) {")
            file.write("\n        if (handle == Constants.NULL) {")
            file.write("\n            throw new NullPointerException(\"{0} handle is NULL in get_{1}\");".format(clasz, name))
            file.write("\n        }\n")
            file.write("\n        return allocator.getByteBuffer(handle)")
            file.write("\n            .{0}(Utils.getBufferIndex(handle) + {1}_OFFSET);".format(primitive_type_2_get[typ], name))
            file.write("\n    }\n")
        else:
            error("Type {0} is not supported".format(typ))


def gen_object(package, obj):
    file = sys.stdout
    
    props = obj['properties']
    file.write("package {0};\n\n".format(package))
    gen_imports(props, package, file)
    file.write("\npublic final class {0} {{\n".format(obj['class']))
    gen_variables(props, file)
    gen_sizeof(props, file)
    gen_create(props, file)
    gen_getkey(obj, file)
    gen_foreach()
    gen_getters(obj['class'], props, file)
    file.write("}\n");
    
    # file.close()


def gen_objects(cfg_json):
    p = cfg_json['package']
    for o in cfg_json['objects']:
        gen_object(p, o)


def gen_mmap_file_variables(properties, file):
    for p in properties:
        name = p['name']
        if isinstance(p['type'], dict):
            typ = p['type']['type']
            of = p['type']['of']
            if typ == "hashmap":
                file.write("    private final {0} {1};\n".format(get_hashmap_name(of), name))
            else:
                error("Complex type {0} of {1} is not supported".format(typ, of))
        elif p['type'] in primitive_types:
            file.write("    private {0} {1};\n".format(p['type'], name))
        else:
            error("Type {0} is not supported".format(p['type']))


def get_hashmap_base_n():
    return "MUHTObjLong"

def get_hashmap_base_fqn():
    return "sk.util." + get_hashmap_base_n()


def get_hashmap_name(of):
    return "MUHT" + of


def gen_hashmap(pkg, of, file):
    file.write("package {0};\n\n".format(pkg))
    file.write("import sk.mmap.IUnsafeAllocator;\n")
    file.write("import {0};\n".format(get_hashmap_base_fqn()))
    file.write("\n")
    file.write("import {0}.{1};\n".format(pkg, of))
    file.write("\n")
    file.write("public class {0} extends {1} {{\n".format(get_hashmap_name(of), get_hashmap_base_n()))
    file.write("    public {0}(final IUnsafeAllocator allocator, final int numBuckets) {{\n".format(get_hashmap_name(of)))
    file.write("        super(allocator, numBuckets);\n")
    file.write("    }\n")
    file.write("    protected long getId(final IUnsafeAllocator allocator, final long handle) {\n")
    file.write("        return {0}.getKey(allocator, handle);\n".format(of))
    file.write("    }\n")
    file.write("}\n")


def gen_mmap_containers(pkg, properties, file):
    for p in properties:
        if isinstance(p['type'], dict):
            typ = p['type']['type']
            of = p['type']['of']
            if typ == "hashmap":
                gen_hashmap(pkg, of, file)
            else:
                error("Complex type {0} of {1} is not supported".format(typ, of))


def get_mmap_constructor(clasz, properties, file):
    file.write("\n    public {0}(final IUnsafeAllocator allocator".format(clasz))
    # Generate parameter list based on properties of this class
    for p in properties:
        name = p['name']
        typ = p['type']
        if isinstance(typ, dict):
            typ = p['type']['type']
            of = p['type']['of']
            if typ == "hashmap":
                file.write(", final int {0}_buckets".format(name))
            else:
                error("Complex type {0} of {1} is not supported".format(typ, of))
        elif typ in primitive_types:
            file.write(", final {0} {1}".format(typ, name))
        else:
            error("Type {0} is not supported".format(typ))

    file.write(") {\n")
    file.write("        this.allocator = allocator;\n")
    for p in properties:
        name = p['name']
        typ = p['type']
        if isinstance(typ, dict):
            typ = p['type']['type']
            of = p['type']['of']
            if typ == "hashmap":
                file.write("        {0} = new {1}(allocator, {0}_buckets);\n".format(name, get_hashmap_name(of)))
            else:
                error("Complex type {0} of {1} is not supported".format(typ, of))
        elif typ in primitive_types:
            file.write("        this.{0} = {0};\n".format(name))
        else:
            error("Type {0} is not supported".format(typ))

    file.write("    }\n")


def get_getter_setter_primitive(name, typ, file):
    file.write("\n    public void set_{0}(final {1} {0}) {{\n".format(name, typ))
    file.write("        this.{0} = {0};\n".format(name))
    file.write("    }\n")
    file.write("\n    public {0} get_{1}() {{\n".format(typ, name))
    file.write("        return this.{0};\n".format(name))
    file.write("    }\n")


def get_get_put_hashmap(name, of, file):
    # TODO: Support hashmap of primitive types.
    file.write("\n    public void put_{0}(final long id, final long obj) {{\n".format(name))
    file.write("        {0}.put(id, obj);\n".format(name))
    file.write("    }\n")
    file.write("\n    public long get_{0}(final long id) {{\n".format(name))
    file.write("        return this.{0}.get(id);\n".format(name))
    file.write("    }\n")


def gen_mmap_functions(properties, file):
    for p in properties:
        name = p['name']
        if isinstance(p['type'], dict):
            typ = p['type']['type']
            of = p['type']['of']
            if typ == "hashmap":
                get_get_put_hashmap(name, of, file)
            else:
                error("Complex type {0} of {1} is not supported".format(typ, of))
        elif p['type'] in primitive_types:
            get_getter_setter_primitive(name, p['type'], file)
        else:
            error("Type {0} is not supported".format(p['type']))


def gen_mmap_file(cfg_json):
    file = sys.stdout

    mmap_file = cfg_json['mmap_file']
    props = mmap_file['properties']
    gen_mmap_containers(cfg_json['package'], props, file)
    file.write("package {0};\n\n".format(cfg_json['package']))
    gen_imports(props, cfg_json['package'], file)
    file.write("\npublic final class {0} {{\n".format(mmap_file['class']))
    file.write("\n    private final IUnsafeAllocator allocator;\n")
    gen_mmap_file_variables(props, file)
    get_mmap_constructor(mmap_file['class'], props, file)
    gen_mmap_functions(props, file)
    file.write("}\n")
    
    # file.close()


cfg_file = "/Users/soumitra/opensource/mmap-ds/fsimage.json"
cfg_json = get_json(cfg_file)

gen_objects(cfg_json)
gen_mmap_file(cfg_json)

