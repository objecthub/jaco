class M {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public M(java.lang.Object $encl) {
      super();
      this.$encl = $encl;
   }
   O $nes0;
   M $self;
   public class $Propagator extends java.lang.Object implements $M {
      public M $M() {
         return $self;
      }
      public O $O() {
         return $nes0;
      }
   }
   public class $Configurator extends java.lang.Object implements $M {
      public M $M() {
         return $self;
      }
      public O $O() {
         return $nes0;
      }
   }
   public O $create$O() {
      return new O($prop);
   }
   public void $main(String[] args) {
      System.out.println("Running main... " + $nes0);
      $nes0.$access();
      System.out.println("Accessed O");
      $nes0.$access().$prop.$R().$access().foo();
   }
   public M() {
      super();
   }
   public synchronized M $access() {
      System.out.println();
      if ($self == null) {
         this.$self = this;
         $nes0 = $create$O();
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
      this.$nes0 = $c.$O();
   }
   protected void $init() {
   }
   public static void main(java.lang.String[] args) {
      M m = new M(null);
      System.out.println("created M");
      m.$access();
      System.out.println("accessed M");
      m.$main(args);
   }
}
interface $M {
   public abstract M $M();
}
class Mn extends M {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public Mn(java.lang.Object $encl) {
      super($encl);
      this.$encl = $encl;
   }
   On $nes0;
   Mn $self;
   public class $Propagator extends java.lang.Object implements $Mn {
      public M $M() {
         return $self;
      }
      public Mn $Mn() {
         return $self;
      }
      public O $O() {
         return $nes0;
      }
      public On $On() {
         return $nes0;
      }
   }
   public class $Configurator extends M.$Configurator implements $Mn {
      public M $M() {
         return $self;
      }
      public Mn $Mn() {
         return $self;
      }
      public O $O() {
         return $nes0;
      }
      public On $On() {
         return $nes0;
      }
   }
   public On $create$On() {
      return new On($prop);
   }
   public Mn() {
      super();
   }
   public synchronized M $access() {
      System.out.println();
      if ($self == null) {
         this.$self = this;
         $nes0 = $create$On();
         super.$connect(new $Configurator());
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
      this.$nes0 = $c.$On();
      super.$connect(new $Configurator());
   }
   protected void $init() {
      super.$init();
   }
   public static void main(java.lang.String[] args) {
      Mn m = new Mn(null);
      System.out.println("created M");
      m.$access();
      System.out.println("accessed M");
      m.$main(args);
   }
}
interface $Mn {
   public abstract Mn $Mn();
}
class O {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public O(java.lang.Object $encl) {
      super();
      this.$encl = $encl;
   }
   R $nes0;
   O $self;
   public class $Propagator extends java.lang.Object implements $O {
      public O $O() {
         return $self;
      }
      public R $R() {
         return $nes0;
      }
   }
   public class $Configurator extends java.lang.Object implements $O {
      public O $O() {
         return $self;
      }
      public R $R() {
         return $nes0;
      }
   }
   public R $create$R() {
      return new R($prop);
   }
   public O() {
      super();
   }
   public synchronized O $access() {
      System.out.println("(accessing " + this + ")");
      if ($self == null) {
         this.$self = this;
         $nes0 = $create$R();
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
      this.$nes0 = $c.$R();
   }
   protected void $init() {
   }
   public static void main(java.lang.String[] args) {
      throw new Error("module not executable");
   }
}
interface $O {
   public abstract O $O();
}
class On extends O {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public On(java.lang.Object $encl) {
      super($encl);
      this.$encl = $encl;
   }
   Rn $nes0;
   On $self;
   public class $Propagator extends java.lang.Object implements $On {
      public O $O() {
         return $self;
      }
      public On $On() {
         return $self;
      }
      public R $R() {
         return $nes0;
      }
      public Rn $Rn() {
         return $nes0;
      }
   }
   public class $Configurator extends O.$Configurator implements $On {
      public O $O() {
         return $self;
      }
      public On $On() {
         return $self;
      }
      public R $R() {
         return $nes0;
      }
      public Rn $Rn() {
         return $nes0;
      }
   }
   public Rn $create$Rn() {
      return new Rn($prop);
   }
   public On() {
      super();
   }
   public synchronized O $access() {
      System.out.println("Accessing " + this);
      if ($self == null) {
         this.$self = this;
         $nes0 = $create$Rn();
         super.$connect(new $Configurator());
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
      this.$nes0 = $c.$Rn();
      super.$connect(new $Configurator());
   }
   protected void $init() {
      super.$init();
   }
   public static void main(java.lang.String[] args) {
      throw new Error("module not executable");
   }
}
interface $On {
   public abstract On $On();
}
class R {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public R(java.lang.Object $encl) {
      super();
      this.$encl = $encl;
   }
   R $self;
   public class $Propagator extends java.lang.Object implements $R {
      public R $R() {
         return $self;
      }
   }
   public class $Configurator extends java.lang.Object implements $R {
      public R $R() {
         return $self;
      }
   }
   void foo() {
      System.out.println("R.foo");
   }
   public R() {
      super();
   }
   public synchronized R $access() {
      System.out.println();
      if ($self == null) {
         this.$self = this;
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
   }
   protected void $init() {
   }
   public static void main(java.lang.String[] args) {
      throw new Error("module not executable");
   }
}
interface $R {
   public abstract R $R();
}
class Rn extends R {
   $Propagator $prop = new $Propagator();
   java.lang.Object $encl;
   public Rn(java.lang.Object $encl) {
      super($encl);
      this.$encl = $encl;
   }
   Rn $self;
   public class $Propagator extends java.lang.Object implements $Rn {
      public R $R() {
         return $self;
      }
      public Rn $Rn() {
         return $self;
      }
   }
   public class $Configurator extends R.$Configurator implements $Rn {
      public R $R() {
         return $self;
      }
      public Rn $Rn() {
         return $self;
      }
   }
   void foo() {
      System.out.println("Rn.foo");
   }
   public Rn() {
      super();
   }
   public synchronized R $access() {
      System.out.println();
      if ($self == null) {
         this.$self = this;
         super.$connect(new $Configurator());
         $init();
         return this;
      } else 
         return this;
   }
   protected void $connect($Configurator $c) {
      this.$self = this;
      super.$connect(new $Configurator());
   }
   protected void $init() {
      super.$init();
   }
   public static void main(java.lang.String[] args) {
      throw new Error("module not executable");
   }
}
interface $Rn {
   public abstract Rn $Rn();
}
