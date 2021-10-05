package ua.ddovgal.baklazhan.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.TypeUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionUtils {

    /**
     * Returns actual generic types that were used for {@code typesDeclaringClass} by certain {@code typesHavingClass} class.
     *
     * @param typesHavingClass    class for which need to get actual generic types used for {@code typesDeclaringClass}.
     * @param typesDeclaringClass generics specifier class, which actual generic types used by {@code typesDeclaringClass} need to get.
     *
     * @return actual generic types in order as they were indicated in {@code typesDeclaringClass}.
     */
    public static List<Type> getActualTypesForClass(Class<?> typesHavingClass, Class<?> typesDeclaringClass) {
        TypeVariable<? extends Class<?>>[] typeParameters = typesDeclaringClass.getTypeParameters();
        Map<TypeVariable<?>, Type> typeArguments = TypeUtils.getTypeArguments(typesHavingClass, typesDeclaringClass);
        return Arrays.stream(typeParameters).map(typeArguments::get).collect(Collectors.toList());
    }
}
