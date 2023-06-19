package team.jlm.refactoring

class RefactoringImpl<T : BaseRefactoringProcessor>(myProcessor: T) :
    BaseRefactoring<T>(myProcessor)