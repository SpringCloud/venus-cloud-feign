package feign;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;

import static feign.Util.checkState;

public abstract class VenusBaseContract extends Contract.BaseContract {

    @Override
    protected MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
        MethodMetadata data = new MethodMetadata();
        data.returnType(Types.resolve(targetType, targetType, method.getGenericReturnType()));
        data.configKey(Feign.configKey(targetType, method));

        if (targetType.getInterfaces().length == 1) {
            processAnnotationOnClass(data, targetType.getInterfaces()[0]);
        }
        processAnnotationOnClass(data, targetType);


        for (Annotation methodAnnotation : method.getAnnotations()) {
            processAnnotationOnMethod(data, methodAnnotation, method);
        }
        checkState(data.template().method() != null,
                "Method %s not annotated with HTTP method type (ex. GET, POST)",
                method.getName());
        Class<?>[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int count = parameterAnnotations.length;
        for (int i = 0; i < count; i++) {
            boolean isHttpAnnotation = false;
            if (parameterAnnotations[i] != null) {
                isHttpAnnotation = processAnnotationsOnParameter(data, parameterAnnotations[i], i);
            }
            if (parameterTypes[i] == URI.class) {
                data.urlIndex(i);
            } else if (!isHttpAnnotation) {
//                checkState(data.formParams().isEmpty(),
//                        "Body parameters cannot be used with form parameters.");
                checkState(data.bodyIndex() == null, "Method has too many Body parameters: %s", method);
                data.bodyIndex(i);
                data.bodyType(Types.resolve(targetType, targetType, genericParameterTypes[i]));
            }
        }

        if (data.headerMapIndex() != null) {
            checkMapString("HeaderMap", parameterTypes[data.headerMapIndex()], genericParameterTypes[data.headerMapIndex()]);
        }

        if (data.queryMapIndex() != null) {
            checkMapString("QueryMap", parameterTypes[data.queryMapIndex()], genericParameterTypes[data.queryMapIndex()]);
        }

        return data;
    }

    private static void checkMapString(String name, Class<?> type, Type genericType) {
        checkState(Map.class.isAssignableFrom(type),
                "%s parameter must be a Map: %s", name, type);
        Type[] parameterTypes = ((ParameterizedType) genericType).getActualTypeArguments();
        Class<?> keyClass = (Class<?>) parameterTypes[0];
        checkState(String.class.equals(keyClass),
                "%s key must be a String: %s", name, keyClass.getSimpleName());
    }
}