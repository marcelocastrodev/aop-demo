package com.marcelocastro.aspect;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
@Slf4j
public class LogRequestResponseAspect {
  @Around("annotatedMethod() || publicMethodsWithinAnnotatedClass()")
  public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info(joinPoint.getSignature() + " Request arguments: {}", joinPoint.getArgs());
    Object proceed = joinPoint.proceed();
    log.info(joinPoint.getSignature() + " Response: {} ", Objects.isNull(proceed) ? "{}" : proceed);
    return proceed;
  }

  @Pointcut("within(@com.marcelocastro.util.LogRequestResponse *)")
  public void beanAnnotated() {}

  @Pointcut("execution(public * *(..))")
  public void publicMethod() {}

  @Pointcut("beanAnnotated() && publicMethod()")
  public void publicMethodsWithinAnnotatedClass() {}

  @Pointcut("@annotation(com.marcelocastro.util.LogRequestResponse)")
  public void annotatedMethod() {}
}
