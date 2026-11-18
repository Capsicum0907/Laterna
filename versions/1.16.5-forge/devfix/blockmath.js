var RENAMES = {
    'func_227987_b_': 'inverse',
    'func_227985_a_': 'compose'
};

function initializeCoreMod() {
    return {
        'laterna_devfix_blockmath': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.core.BlockMath'
            },
            'transformer': function (node) {
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                node.methods.forEach(function (method) {
                    var instructions = method.instructions.toArray();
                    for (var i = 0; i < instructions.length; i++) {
                        var instruction = instructions[i];
                        if (instruction.getOpcode() === Opcodes.INVOKEVIRTUAL
                            && instruction.owner === 'com/mojang/math/Transformation'
                            && RENAMES[instruction.name] !== undefined) {
                            instruction.name = RENAMES[instruction.name];
                        }
                    }
                });
                return node;
            }
        }
    };
}
