module VisitorPattern {
    
    interface Data {
        void accept(Visitor v);
    }

    abstract class Visitor;
    
    
}


module TreeVisitorPattern refines VisitorPattern {

    interface TreeVisitor {
        void Ident(String name);
        void Lambda(String x, Data body);
        void Apply(Data fun, Data arg);
    }

    abstract class Visitor implements TreeVisitor;
    
    class Printer extends Visitor implements TreeVisitor = {
        void Ident(String name) { ... }
        void Lambda(String x, Data body) { ... }
        void Apply(Data fun, Data arg) { ... }
    }
}


module XTreeVisitorPattern refines TreeVisitorPattern {
    
    interface XTreeVisitor {
        void Num(int x);
        void Plus(Data left, Data right);
    }

    abstract class Visitor implements TreeVisitor, XTreeVisitor;
    
    class Printer extends Visitor implements TreeVisitor, XTreeVisitor = super.Printer {
        
    }
    
}
