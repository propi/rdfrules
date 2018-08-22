package com.github.propi.rdfrules.java.algorithm;

import com.github.propi.rdfrules.utils.Debugger$;
import scala.runtime.BoxedUnit;

import java.util.function.Consumer;

/**
 * Created by Vaclav Zeman on 14. 5. 2018.
 */
public class Debugger {

    private final com.github.propi.rdfrules.utils.Debugger debugger;

    private Debugger(com.github.propi.rdfrules.utils.Debugger debugger) {
        this.debugger = debugger;
    }

    public com.github.propi.rdfrules.utils.Debugger asScala() {
        return debugger;
    }

    public static Debugger empty() {
        return new Debugger(com.github.propi.rdfrules.utils.Debugger.EmptyDebugger$.MODULE$);
    }

    public static void use(Consumer<Debugger> f) {
        Debugger$.MODULE$.apply(Debugger$.MODULE$.apply$default$1(), x -> {
            f.accept(new Debugger(x));
            return BoxedUnit.UNIT;
        });
    }

}