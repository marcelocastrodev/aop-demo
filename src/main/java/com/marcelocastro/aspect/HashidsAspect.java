package com.marcelocastro.aspect;

import com.marcelocastro.config.HashidsProperties;
import com.marcelocastro.util.Hasheable;
import com.marcelocastro.util.Hashids;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
@Slf4j
public class HashidsAspect {

  private final org.hashids.Hashids hashids;

  public HashidsAspect(HashidsProperties hashidsProperties) {
    this.hashids = new org.hashids.Hashids(
        hashidsProperties.getSalt(), hashidsProperties.getMinHashLength(), hashidsProperties.getAlphabet());
  }

  @Around("within(com.marcelocastro..controller..*)")
  public Object applyHashids(ProceedingJoinPoint joinPoint) throws Throwable {
    Object[] args = applyHashidsInParameters(joinPoint);
    List<Object> argsHashed = Arrays.stream(args)
        .map(this::applyHashids)
        .toList();
    Object proceed = joinPoint.proceed(argsHashed.toArray());
    applyHashids(proceed);
    return proceed;
  }

  private Object[] applyHashidsInParameters(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getMethod().getName();
    Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();
    Annotation[][] methodParameters = joinPoint.getTarget().getClass().getMethod(methodName, parameterTypes)
        .getParameterAnnotations();
    Object[] jointPointArgs = joinPoint.getArgs();
    for (int i = 0; i < methodParameters.length; i++) {
      String hashidsPrefix = getHashidsPrefix(methodParameters[i]);
      if (Objects.nonNull(hashidsPrefix)) {
        String originalValue = jointPointArgs[i].toString();
        jointPointArgs[i] = applyHashids(originalValue, hashidsPrefix);
        log.debug("Hashids found as parameter of {} method parameter. Before [{}] - After [{}]", methodName,
            originalValue, jointPointArgs[i]);
      }
    }
    return jointPointArgs;
  }

  private String getHashidsPrefix(Annotation[] parameter) {
    String hashidsPrefix = null;
    for (Annotation annotation : parameter) {
      if (annotation.annotationType().equals(Hashids.class)) {
        hashidsPrefix = ((Hashids) annotation).domain().getPrefix();
        break;
      }
    }
    return hashidsPrefix;
  }

  private Object applyHashids(Object object) {
    if (object instanceof Hasheable) {
      Field[] declaredFields = object.getClass().getDeclaredFields();
      for (Field declaredField : declaredFields) {
        Hashids hashidsAnnotation = declaredField.getAnnotation(Hashids.class);
        if (Objects.nonNull(hashidsAnnotation)) {
          applyHashids(object, declaredField, hashidsAnnotation.domain().getPrefix());
        } else {
          applyHashids(getObject(object, declaredField));
        }
      }
    } else if (object instanceof Iterable) {
      ((Iterable<?>) object).iterator().forEachRemaining(this::applyHashids);
    }
    return object;
  }

  private void applyHashids(Object object, Field field, String prefix) {
    String fieldName = field.getName();
    String setter = String.format("set%C%s", fieldName.charAt(0), fieldName.substring(1));
    try {
      Object originalValue = getObject(object, field);
      if (originalValue instanceof String) {
        Method method = object.getClass().getMethod(setter, field.getType());
        String appliedValue = applyHashids(originalValue.toString(), prefix);
        method.invoke(object, appliedValue);
        log.debug("Hashids found in {}.{}... Before [{}] - After [{}]", object.getClass().getName(), fieldName,
            originalValue, appliedValue);
      }
    } catch (NoSuchMethodException e) {
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private String applyHashids(String value, String prefix) {
    return isNumber(value)
        ? prefix.concat("-").concat(hashids.encode(Long.parseLong(value)))
        : toString(hashids.decode(value.substring((prefix + "-").length())));
  }

  private Object getObject(Object object, Field field) {
    String fieldName = field.getName();
    String getter = String.format("get%C%s", fieldName.charAt(0), fieldName.substring(1));
    Object getterObject = null;
    try {
      Method method = object.getClass().getMethod(getter);
      getterObject = method.invoke(object);
    } catch (NoSuchMethodException e) {
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return getterObject;
  }

  private static boolean isNumber(String str) {
    boolean blnNumber = false;
    try {
      Double.parseDouble(str);
      blnNumber = true;
    } catch (NumberFormatException nfe) {
    }
    return blnNumber;
  }

  private String toString(long[] longs) {
    StringBuilder buff = new StringBuilder();
    for (long l : longs) {
      buff.append(l);
    }
    return buff.toString();
  }
}
