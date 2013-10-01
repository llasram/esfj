package esfj;

import java.lang.annotation.*;
import javax.inject.Qualifier;
import clojure.lang.IFn;
import org.grouplens.lenskit.core.Parameter;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Qualifier
@Parameter(IFn.class)
public @interface Factory {
}
