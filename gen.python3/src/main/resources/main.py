import ast
import json
import asttokens
import sys
import os
from pathlib import Path
from argparse import ArgumentParser
from itertools import repeat

LITERALS = [
    ast.Name,
    ast.Num,
    ast.Str,
    ast.Constant,
]
LABEL = {
    ast.FunctionDef: 'name',
    ast.ClassDef: 'name',
    ast.arg: 'arg',
    ast.alias: ['asname', 'label', 'name'],
    ast.Name: 'label',
}


def convertToNumber(s):
    return int.from_bytes(s.encode(), 'little')


def astTraverser(atok, node, d=None):
    if d is None:
        d = dict()
    if isinstance(node, ast.AST):
        d['typeLabel'] = type(node).__name__
        d['type'] = convertToNumber(d['typeLabel'])
        d['start'], d['end'] = atok.get_text_range(node)
    if isinstance(node, ast.expr) or isinstance(node, ast.stmt):
        d['col_offset'] = node.col_offset
        d['lineno'] = node.lineno
    if any(map(lambda k, it: isinstance(it, k), LITERALS, repeat(node))):
        d['label'] = atok.get_text(node)
        d['children'] = list()
        return d
    if node.__class__ in LABEL:
        labels = LABEL[node.__class__]
        if type(LABEL[node.__class__]) is not list:
            labels = [labels]
        while d.get('label') is None and len(labels) > 0:
            try:
                d['label'] = getattr(node, labels.pop(0))
            except AttributeError:
                pass
            except IndexError:
                pass
    d['children'] = list()
    for field in node._fields:
        value = getattr(node, field)
        if value is None:
            continue
        if isinstance(value, ast.AST):
            d['children'].append(astTraverser(atok, value))
        elif isinstance(value, list):
            d['children'] += [astTraverser(atok, it) for it in value]
        else:
            d[field] = value
    return d


def main(inputStream, outputStream, pretty):
    source = inputStream.read()
    atok = asttokens.ASTTokens(source, parse=True)
    dumps = astTraverser(atok, atok.tree)
    indent = None
    if pretty:
        indent = 2
    json.dump(dumps, outputStream, indent=indent)

if __name__ == '__main__':
    parser = ArgumentParser(description="")
    parser.add_argument('-i', '--input')
    parser.add_argument('-o', '--output')
    parser.add_argument('-p', '--pretty', action='store_true')
    args = parser.parse_args()
    if args.input is not None:
        path = Path(args.input)
        inputStream = open(str(path.resolve()), mode='r')
    else:
        inputStream = sys.stdin

    if args.output is not None:
        path = Path(args.output)
        outputStream = open(str(path.resolve()), mode='w')
    else:
        outputStream = sys.stdout

    main(inputStream, outputStream, args.pretty)
