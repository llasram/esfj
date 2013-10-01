package esfj;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;
import javax.inject.Inject;
import javax.inject.Provider;

public class ExampleProvider implements Provider<String> {
    private final IFn factory;

    @Inject
    public ExampleProvider(@Factory IFn factory, String arg0, String arg1) {
        this.factory = (IFn) factory.invoke(arg0, arg1);
    }

    public String get() {
        return (String) factory.invoke();
    }
}
