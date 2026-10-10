package com.northwind.springbasics.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** A cross-cutting concern: times every public method in the service package. */
@Aspect
@Component
public class TimingAspect {

    @Around("execution(public * com.northwind.springbasics.service..*(..))")
    public Object time(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        try {
            return call.proceed();                       // invoke the real method
        } finally {
            long micros = (System.nanoTime() - start) / 1_000;
            System.out.printf("[timing] %s took %d us%n", call.getSignature().toShortString(), micros);
        }
    }
}
