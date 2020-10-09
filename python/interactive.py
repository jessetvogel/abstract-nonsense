import sys
from core import *
from parser import *

G = recreate_global_diagram()

stream = Stream(sys.stdin)
lexer = Lexer(stream)
parser = Parser(lexer)
parser.book = G

while True:
    # Reset sys.stdin
    sys.stdin.seek(0)
    
    try:
        parser.parse()
    except Exception as e:
        print(str(e))
