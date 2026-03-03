package com.demandlane.exception;

import com.demandlane.dto.ErrorCode;
import com.demandlane.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class GlobalExceptionMapper {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @ServerExceptionMapper
    public Response mapNotFound(EntityNotFoundException ex) {
        LOG.debugf("Entity not found: %s", ex.getMessage());
        return Response.status(404)
                .entity(ErrorResponse.of(ErrorCode.ENTITY_NOT_FOUND, ex.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response mapDuplicate(DuplicateEntityException ex) {
        LOG.warnf("Duplicate entity: %s", ex.getMessage());
        return Response.status(409)
                .entity(ErrorResponse.of(ErrorCode.DUPLICATE_ENTITY, ex.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response mapBorrowingRule(BorrowingRuleException ex) {
        LOG.warnf("Borrowing rule violation: %s", ex.getMessage());
        return Response.status(409)
                .entity(ErrorResponse.of(ErrorCode.BORROWING_RULE_VIOLATION, ex.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response mapActiveLoan(ActiveLoanException ex) {
        LOG.warnf("Active loan constraint: %s", ex.getMessage());
        return Response.status(409)
                .entity(ErrorResponse.of(ErrorCode.ACTIVE_LOAN_CONFLICT, ex.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response mapConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        LOG.warnf("Validation error: %s", msg);
        return Response.status(400)
                .entity(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, msg))
                .build();
    }

    @ServerExceptionMapper
    public Response mapGeneric(Exception ex) {
        if (ex instanceof WebApplicationException webEx) {
            return webEx.getResponse();
        }
        LOG.errorf(ex, "Unexpected error");
        return Response.status(500)
                .entity(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Internal server error"))
                .build();
    }
}
