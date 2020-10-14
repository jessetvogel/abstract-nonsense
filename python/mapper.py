#!/usr/bin/env python
# coding: utf-8

# In[1]:


import sys
from core import *
from parser import *


# In[41]:


class Mapper:
    
    def __init__(self, context, target):
        self.context = context
        self.target = target
        self.mapping = {}
        self.options = [{}]
        
    def combine_mappings(self, A, B):
        C = A.copy()
        for x in B:
            if x in A and A[x] != B[x]:
                return None
            C[x] = B[x]
        return C
    
    def combine_lists_of_options(self, options_A, options_B):
        options = []
        for B in options_B:
            for A in options_A:
                C = self.combine_mappings(A, B)
                if C != None and C not in options:
                    options.append(C)
        
        return options
        
    # Find all possible ways in which x could equal y
    def find_consequences(self, x, y):
        if x in self.context.data:
            return [{}]
        
        reps_x = [ r for r in self.context.representations if r.ptr == x ]
        reps_y = [ r for r in self.target.representations if r.ptr == y ]
        
        options = []
        for r_x in reps_x:
            for r_y in reps_y:
                m = self.reps_match_consequences(r_x, r_y)
                if m != None and m not in options:
                    options.append(m)
        
        return options
    
    # Returns what the consequence is if r_x matches r_y
    def reps_match_consequences(self, r_x, r_y):
        if type(r_x) != type(r_y):
            return None
        
        if isinstance(r_x, Repr_Property):
            if r_x.prop != r_y.prop:
                return False

        m = {}
        r_x_deps = r_x.dependencies()
        r_y_deps = r_y.dependencies()
        
        for u, v in zip(r_x_deps, r_y_deps):
            if self.context.owns(u):
                m[u] = v
                
        return m
    
    def mapping_set_multiple(self, M):
        for x in M:
            if not self.mapping_set(x, M[x]):
                return False
        return True
    
    def mapping_set(self, x, y):
#         print('Want to map {} --> {}'.format(self.context.str_x(x), self.target.str_x(y)))
        
        # If x was already mapped, make sure it was mapped to y
        if x in self.mapping:
            return self.mapping[x] == y
        
        self.mapping[x] = y
        
        # Induced mapping for category, domain, codomain (that is, if they need to be mapped)
        if self.context.owns(x.category) and not self.mapping_set(x.category, y.category):
            return False
        
        if not x.is_object():
            if self.context.owns(x.domain) and not self.mapping_set(x.domain, y.domain):
                return False

            if self.context.owns(x.codomain) and not self.mapping_set(x.codomain, y.codomain):
                return False
        
        # If x is data, stop here. TODO: maybe already map everything that depends on what is currently mapped
        if x in self.context.data:
            return True
        
        
        
        # See what the consequences are from mapping x to y
        x_options = self.find_consequences(x, y)
        
        # If there is just one x_option, immediately update mapping
        if len(x_options) == 1:            
            if not self.mapping_set_multiple(x_options[0]):
                return False
        
        # Combine old options with the new options
        self.options = self.combine_lists_of_options(self.options, x_options)
                        
        # If there are no options, return False
        if not self.options:
            return False
        
        # Again, if there is just one option, immediately update mapping
        if len(self.options) == 1:
            if not self.mapping_set_multiple(self.options[0]):
                return False
                
            # Also reset options
            self.options = [{}]
            
        return True
    
    def validate(self):
        # All data must be mapped
        if any(x not in self.mapping for x in self.context.data):
#             print('Not all data is mapped yet!')
            return False
        
        # Check if all representations are well-mapped (possibly extend if not yet done)
        r_todo = self.context.representations.copy()
        updates = True
        while updates:
            updates = False
            r_done = []
            for r in r_todo:
                # If r is a representation of some datum, nothing to check
                if r.ptr in self.context.data:
                    r_done.append(r)
                    continue
                    
                # Can only extend the mapping to r if all its dependencies are already mapped
                if any(x not in self.mapping and self.context.owns(x) for x in r.dependencies()):
                    continue
                
                y = self.target.create_from_placeholders(r, self.mapping)

                # If r.ptr was already mapped to something other than y, return False
                if r.ptr in self.mapping and self.mapping[r.ptr] != y:
#                     print('Unsure about mapping of {}: either {} or {}'.format(self.context.str_x(r.ptr), self.target.str_x(self.mapping[r.ptr]), self.target_str_x(y)))
                    return False
                
                self.mapping[r.ptr] = y
                r_done.append(r)
                updates = True
            
            r_todo = [ r for r in r_todo if r not in r_done ]
        
        # At this point, everything is well-mapped!
            
        # Now, verify the conditions
        for C in self.context.conditions:
            if not self.target.has_instance_of(self.mapping[C]):
#                 print('Condition {} could not be verified!'.format(self.context.str_x(C)))
                return False
                
        # Done
        return True
    
    def find_candidates(self, x):
        C = self.mapping[x.category] if x.category in self.mapping else x.category
        if x.is_object():
            # Objects must be mapped to objects
#             print('Look for objects in category {}'.format(self.target.str_x(C)))
            return [ y for y in self.target.morphisms if y.is_object() and y.category == C ]
        else:
            candidates = [ y for y in self.target.morphisms if y.category == C ]

            dom = (self.mapping[x.domain] if x.domain in self.mapping else None) if self.context.owns(x.domain) else x.domain
            cod = (self.mapping[x.codomain] if x.codomain in self.mapping else None) if self.context.owns(x.codomain) else x.codomain

            if dom != None:
                candidates = [ y for y in candidates if y.domain == dom ]
            if cod != None:
                candidates = [ y for y in candidates if y.codomain == cod ]

            return candidates
    
    def find_mapping(self):
        # Consider first datum x which is not yet mapped
        for x in self.context.data:
            if x in self.mapping:
                continue
                
            # Find candidates y for x
            candidates = self.find_candidates(x)
            
            # If there are no candidates, return False
            if not candidates:
#                 print('No candidates found for {}'.format(self.context.str_x(x)))
                return False
            
            # If there is just one candidate, must use that one
            if len(candidates) == 1:
                if not self.mapping_set(x, candidates[0]):
                    return False
                continue
            
            # Branch out?
            for y in candidates:
                M = Mapper(self.context, self.target)
                M.mapping = self.mapping.copy()
                M.mapping_set(x, y)
                if M.find_mapping():
                    self.mapping.update(M.mapping)
                    return True
            
            # If none of the candidates worked, return False
            return False
        
        # If all data was mapped, it is only left to validate the mapping
        return self.validate()
