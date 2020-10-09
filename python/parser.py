#!/usr/bin/env python
# coding: utf-8

# In[1]:


import re

from core import *


# In[2]:


class Stream():
    
    def __init__(self, file):
        self.file = file
        self.line = 1
        self.position = 0
        
    def get(self):
        c = self.file.read(1)
        if c:
            if c == '\n':
                self.line += 1
                self.position = 0
            else:
                self.position += 1
        return c


# In[3]:


class Token:

    T_IDENTIFIER = 0
    T_KEYWORD = 1
    T_SEPARATOR = 2
    T_EOF = 3
    T_NEWLINE = 4
    
    # identifiers:
    # keywords: id, dom, cod, cat
    # separators: (, ), =, =>, ->, ~>,  

    def __init__(self, T, line, position, data = ''):
        self.type = T
        self.line = line
        self.position = position
        self.data = data


# In[4]:


class Lexer():
    
    def __init__(self, stream):
        self.stream = stream
        self.current_token = None
        self.tmp = ''
        self.tmp_line = 1
        self.tmp_position = 0
        
    def tokenize(self, expr):
        if expr in ['id', 'dom', 'cod', 'cat', 'let', 'assume', 'property', 'theorem', 'given', 'with', 'then', 'exists', 'whats']:
            return Token(Token.T_KEYWORD, self.tmp_line, self.tmp_position, expr)

        if expr in ['(', ')', '{', '}', '=', '.', ',', ':', ';', '->', '~>', '=>']:
            return Token(Token.T_SEPARATOR, self.tmp_line, self.tmp_position, expr)
        
        if expr in ['\n']:
            return Token(Token.T_NEWLINE, self.tmp_line, self.tmp_position)
        
        if re.match(r'\A\w+\Z', expr):
            return Token(Token.T_IDENTIFIER, self.tmp_line, self.tmp_position, expr)
                    
        return None
    
    def get_token(self):
        # Read characters until a new Token is produced
        while True:
            c = self.stream.get()

            # End of file
            if not c:
                if self.tmp == '':
                    return Token(Token.T_EOF, -1, -1, '?') # TODO
                    
                token = self.tokenize(self.tmp)
                if not token:
                    raise Exception('Unexpected token \'{}\''.format(self.tmp))
                self.current_token = None
                self.tmp = ''
                return token
            
            # Whitespace: always marks the end of a token (if there currently is one)
            if c in [' ', '\t']:
                if self.tmp == '':
                    continue

                if not self.current_token:
                    raise Exception('Unknown token \'{}\''.format(self.tmp))
                
                token = self.current_token
                self.current_token = None
                self.tmp = ''
                return token
            
            # Comments: marks the end of a token (if there currently is one),
            # then continue discarding characters until a newline appears
            if c in ['#']:
                token = None
                
                if self.tmp != '':
                    if not self.current_token:
                        raise Exception('Unknown token \'{}\''.format(self.tmp))
                    token = self.current_token
                
                while self.stream.get() != '\n':
                    pass
                
                self.tmp = '\n'
                self.tmp_line = self.stream.line
                self.tmp_position = self.stream.position
                self.current_token = self.tokenize(self.tmp)
                
                return token if token else self.get_token()
            
            # Try to enlarge the token if possible
            token = self.tokenize(self.tmp + c)
            if token:
                self.current_token = token
                if self.tmp == '':
                    self.tmp_line = self.stream.line
                    self.tmp_position = self.stream.position
                self.tmp += c
                continue

            # If we also did not succeed before, hope that it will make sense later
            if not self.current_token:
                self.tmp += c
                continue

            # Return the last valid token
            token = self.current_token
            self.tmp = c
            self.tmp_line = self.stream.line
            self.tmp_position = self.stream.position
            self.current_token = self.tokenize(self.tmp)
            return token


# In[5]:


class ParsingError(Exception):
    
    def __init__(self, message, token):
        self.message = message
        self.token = token
        
class InterpretationError(Exception):
    
    def __init__(self, message, token):
        self.message = message
        self.token = token


# In[11]:


class Parser:
    
    def __init__(self, lexer):
        self.lexer = lexer
        self.current_token = None
        
        self.book = None
    
    def next_token(self):
        self.current_token = self.lexer.get_token()
        
        if self.current_token.type == Token.T_NEWLINE:
            self.next_token()
    
    def found(self, token_type, data = None):
        return self.current_token.type == token_type and (data == None or data == self.current_token.data)
        
    def consume(self, token_type = None, data = None):            
        if token_type == None or self.found(token_type, data):
            token = self.current_token
            self.next_token()
            return token
        else:
            raise ParsingError('Expected \'{}\' but found \'{}\''.format(data, self.current_token.data), self.current_token)
    
    def parse(self):
        self.next_token()
        
        while not self.found(Token.T_EOF):
            self.parse_statement(self.book)
        
        self.consume(Token.T_EOF)
        
    # ----------------------------------------------------------------
        
    def parse_statement(self, book):
        # STATEMENT = 
        #  ; |
        #  let LIST_OF_IDENTIFIERS : TYPE |
        #  assume OBJECT |
        #  property IDENTIFIER { GIVENS CONDITIONS } |
        #  theorem IDENTIFIER { GIVENS CONDITIONS CONCLUSIONS } |
        #
        #  whats OBJECT
               
        if self.found(Token.T_SEPARATOR, ';'):
            self.consume()
            return True
            
        if self.found(Token.T_KEYWORD, 'let'):
            self.consume()
            identifiers = self.parse_list_of_identifiers()
            self.consume(Token.T_SEPARATOR, ':')
            m_type = self.parse_type(book)

            if len(m_type) == 1: # Object
                for i in identifiers:
                    book.create_object(m_type[0], i)
            else: # Morphism
                for i in identifiers:
                    book.create_morphism(m_type[0], m_type[1], i, covariant = m_type[2])                
            
            return True
        
        if self.found(Token.T_KEYWORD, 'assume'):
            self.consume()
            t = self.current_token
            C = self.parse_object(book)
            if not C.is_category():
                raise InterpretationError('Assume requires a category!', t)
            book.create_object(C)
            return True
        
        if self.found(Token.T_KEYWORD, 'property'):
            self.consume()
            t_identifier = self.consume(Token.T_IDENTIFIER)
            name = t_identifier.data
            if book.has_property(name) or book.has_symbol(name):
                raise InterpretationError('Name \'{}\' already used'.format(name), t_identifier)
            prop = Property(name)
            prop.add_reference(book)
            
            self.consume(Token.T_SEPARATOR, '{')
            self.parse_givens(prop)
            self.parse_conditions(prop)
            self.consume(Token.T_SEPARATOR, '}')
            
            book.add_property(name, prop)
            return True
        
        if self.found(Token.T_KEYWORD, 'theorem'):
            self.consume()
            t_identifier = self.consume(Token.T_IDENTIFIER)
            name = t_identifier.data
            if book.has_theorem(name):
                raise InterpretationError('Name \'{}\' already used'.format(name), t_identifier)
            thm = Theorem()
            thm.add_reference(book)
            
            self.consume(Token.T_SEPARATOR, '{')
            self.parse_givens(thm)
            self.parse_conditions(thm)
            self.parse_conclusions(thm.conclusion)
            self.consume(Token.T_SEPARATOR, '}')
            
            book.add_theorem(name, thm)
            return True
        
        if self.found(Token.T_KEYWORD, 'whats'):
            self.consume()
            t = self.current_token
            x = self.parse_object(book)
            if x.is_object():
                print('{} : {}'.format(book.str_x(x), book.str_x(x.category)))
            else:
                covariant = x.covariant if x.is_functor() else True
                print('{} : {} {} {}'.format(book.str_x(x), book.str_x(x.domain), '->' if covariant else '~>', book.str_x(x.codomain)))
            
            return True            
        
        raise ParsingError('Unable to parse statement', self.current_token)
    
    def parse_givens(self, context):
        # GIVENS =
        #   GIVEN { GIVEN }
        # 
        # GIVEN =
        #   given LIST_OF_IDENTIFIERS : TYPE

        if not self.found(Token.T_KEYWORD, 'given'):
            raise ParsingError('Expected \'given\': property must have some data', self.current_token)
        
        while self.found(Token.T_KEYWORD, 'given'):
            t_given = self.consume()
            identifiers = self.parse_list_of_identifiers()
            for i in identifiers:
                if not context.is_name_available(i):
                    raise InterpretationError('Name \'{}\' is already used', t_given)
            
            self.consume(Token.T_SEPARATOR, ':')
            m_type = self.parse_type(context)

            if len(m_type) == 1: # case Object
                for i in identifiers:
                    x = context.create_object(m_type[0], i)
                    context.add_data(x)
            else: # case Morphism
                for i in identifiers:
                    x = context.create_morphism(m_type[0], m_type[1], i, covariant = m_type[2])
                    context.add_data(x)
    
    def parse_conditions(self, context):
        # CONDITIONS =
        #   CONDITION { CONDITION }
        # 
        # CONDITION =
        #   with OBJECT
        
        while self.found(Token.T_KEYWORD, 'with'):
            self.consume()
            t_condition = self.current_token
            C = self.parse_object(context)
            if not C.is_category():
                raise InterpretationError('Condition must be a category!', t_condition)
            
            context.add_condition(C)
    
    def parse_conclusions(self, conclusion):
        # CONCLUSIONS =
        #   CONCLUSION { CONCLUSION }
        # 
        # CONCLUSION =
        #   then exists LIST_OF_IDENTIFIERS : TYPE |
        #   then OBJECT
        
        while self.found(Token.T_KEYWORD, 'then'):
            self.consume()
            
            if self.found(Token.T_KEYWORD, 'exists'):
                self.consume()
                identifiers = self.parse_list_of_identifiers()
                self.consume(Token.T_SEPARATOR, ':')
                m_type = self.parse_type(conclusion)
                if len(m_type) == 1: # Object
                    for i in identifiers:
                        x = conclusion.create_object(m_type[0], i)
                else: # Morphism
                    for i in identifiers:
                        x = conclusion.create_morphism(m_type[0], m_type[1], i, covariant = m_type[2])
            
            else:
                C = self.parse_object(conclusion)
                if not C.is_category():
                    raise InterpretationError('Conclusion must be a category!')
                
                conclusion.create_object(C)
            
    def parse_list_of_identifiers(self):
        # LIST_OF_IDENTIFIERS =
        #   IDENTIFIER { , IDENTIFIER }
        
        identifiers = []
        identifiers.append(self.consume(Token.T_IDENTIFIER).data)
        
        while self.found(Token.T_SEPARATOR, ','):
            self.consume()
            identifiers.append(self.consume(Token.T_IDENTIFIER).data)
        
        return identifiers
    
    def parse_type(self, diagram):
        # TYPE =
        #   OBJECT |
        #   OBJECT -> OBJECT |
        #   OBJECT ~> OBJECT
        
        X = self.parse_object(diagram)
        
        is_arrow = False
        if self.found(Token.T_SEPARATOR, '->'):
            is_arrow = True
            covariant = True
            
        if self.found(Token.T_SEPARATOR, '~>'):
            is_arrow = True
            covariant = False
        
        if is_arrow:
            self.consume()
            Y = self.parse_object(diagram)
            return (X, Y, covariant)
        else:
            return (X,)
        
    def parse_object(self, diagram):
        # OBJECT = 
        #   ( OBJECT ) |
        #   id ( OBJECT ) |
        #   dom ( OBJECT ) |
        #   cod ( OBJECT ) |
        #   cat ( OBJECT ) |
        #   IDENTIFIER ( LIST_OF_OBJECTS ) |
        #   IDENTIFIER |
        #   OBJECT = OBJECT |
        #   OBJECT . OBJECT |
        #   OBJECT => OBJECT | TODO
        #   OBJECT & OBJECT | TODO
        #   OBJECT + OBJECT | TODO
        #   ... more?
        
        x = None
        
        if self.found(Token.T_SEPARATOR, '('):
            self.consume()
            x = self.parse_object(diagram)
            self.consume(Token.T_SEPARATOR, ')')
        
        elif self.found(Token.T_KEYWORD, 'id'):
            self.consume()
            self.consume(Token.T_SEPARATOR, '(')
            x = self.parse_object(diagram)
            if not x.is_object():
                raise InterpretationError('id can only be appled to objects!') # TODO: can it?
            self.consume(Token.T_SEPARATOR, ')')
            
        elif self.found(Token.T_KEYWORD, 'dom'):
            self.consume()
            self.consume(Token.T_SEPARATOR, '(')
            x = self.parse_object(diagram).domain
            self.consume(Token.T_SEPARATOR, ')')
            
        elif self.found(Token.T_KEYWORD, 'cod'):
            self.consume()
            self.consume(Token.T_SEPARATOR, '(')
            x = self.parse_object(diagram).codomain
            self.consume(Token.T_SEPARATOR, ')')

        elif self.found(Token.T_KEYWORD, 'cat'):
            self.consume()
            self.consume(Token.T_SEPARATOR, '(')
            x = self.parse_object(diagram).category
            self.consume(Token.T_SEPARATOR, ')')
            
        elif self.found(Token.T_IDENTIFIER):
            t_identifier = self.consume()
            name = t_identifier.data
            
            if self.found(Token.T_SEPARATOR, '('):
                self.consume()
                objects = self.parse_list_of_objects(diagram)
                self.consume(Token.T_SEPARATOR, ')')
                x = diagram.apply(name, objects)
                if x == None:
                    raise InterpretationError('Failed to apply {} to {}'.format(name, objects), t_identifier)
            else:
                x = diagram.find_morphism(name)
                if x == None:
                    raise InterpretationError('Unknown identifier \'{}\''.format(name), t_identifier)
        
        if x == None:
            raise ParsingError('Expected an object/morphism', self.current_token)
        
        # Now we have some x, see if we can (possibly) extend it!
        # This seems to be the solution to left-recursive patterns
        while True:
            
            if self.found(Token.T_SEPARATOR, '='):
                self.consume()
                y = self.parse_object(diagram)
                x = diagram.create_equality(x, y)
                continue
            
            if self.found(Token.T_SEPARATOR, '.'):
                self.consume()
                y = self.parse_object(diagram)
                x = diagram.create_composition([ x, y ])
                continue
            
            if self.found(Token.T_SEPARATOR, '=>'):
                pass
            
            break
            
        return x
        
    def parse_list_of_objects(self, diagram):
        # LIST_OF_OBJECTS =
        #   OBJECT { , OBJECT }
        
        objects = []
        objects.append(self.parse_object(diagram))
        
        while self.found(Token.T_SEPARATOR, ','):
            self.consume()
            objects.append(self.parse_object(diagram))
        
        return objects


# In[ ]:




