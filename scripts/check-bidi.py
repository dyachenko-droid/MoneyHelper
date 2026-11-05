#!/usr/bin/env python3
"""Reject invisible Unicode direction controls in source without Kotlin PSI/UAST."""
from pathlib import Path
import sys

controls = {0x061C, 0x200E, 0x200F, *range(0x202A, 0x202F), *range(0x2066, 0x206A)}
errors = []
for root in (Path('app/src'), Path('scripts'), Path('.github')):
    for path in root.rglob('*'):
        if path.is_file() and path.suffix in {'.kt', '.java', '.xml', '.json', '.py', '.yml', '.yaml'}:
            for number, line in enumerate(path.read_text(encoding='utf-8').splitlines(), 1):
                if any(ord(ch) in controls for ch in line):
                    errors.append(f'{path}:{number}: invisible Unicode direction control')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    sys.exit(1)
print('No invisible Unicode direction controls in source')
