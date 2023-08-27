package top.kkoishi.stg.script.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * An interface used to resolve the generic parameters' actual types, and please do not implement this class.
 *
 * @author KKoishi_
 */
@SuppressWarnings("unused")
public interface TypeResolver<T> {
    /**
     * Resolve the Type.
     * @return Type
     */
    default Type resolve() {
        return ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
    }
}
