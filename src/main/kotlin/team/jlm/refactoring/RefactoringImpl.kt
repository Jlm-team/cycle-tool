package team.jlm.refactoring

class RefactoringImpl<T : IRefactoringProcessor>(myProcessor: T) :
    BaseRefactoring<T>(myProcessor)