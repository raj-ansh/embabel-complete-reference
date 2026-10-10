package com.northwind.content.web;

import com.embabel.agent.core.support.InvalidLlmReturnTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** The model could not produce a valid object even after Embabel's retries. */
    @ExceptionHandler(InvalidLlmReturnTypeException.class)
    public ProblemDetail invalidModelOutput(InvalidLlmReturnTypeException ex) {
        log.warn("Model output failed validation: {} violation(s)", ex.getConstraintViolations().size());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "The content could not be generated in the required format. Please try again.");
        problem.setTitle("Generation failed");
        return problem;        // never echo the raw model output to the client
    }
}
