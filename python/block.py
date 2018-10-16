from abc import ABC, abstractmethod

# Abstract class that each module should extend. Defines an update method where
# it takes in all of the block inputs each "cycle" and should return the output
# values of that block  
class Block(ABC):

    @abstractmethod
    def update(self):
        raise NotImplementedError

