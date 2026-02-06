import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            let errorMessage = 'Ocurrió un error inesperado';
            let validationErrors = null;

            if (error.status === 400) {
                console.error('Bad Request (400):', error.error);
                // Spring Boot validation errors usually come in a specific format
                // Depending on your GlobalExceptionHandler implementation
                errorMessage = error.error.message || 'Error de validación';
                validationErrors = error.error.errors || error.error.data;
            } else if (error.status === 401) {
                errorMessage = 'Sesión expirada o no autorizada';
            } else if (error.status === 403) {
                errorMessage = 'No tienes permisos para realizar esta acción';
            } else if (error.status === 500) {
                errorMessage = 'Error interno del servidor';
            }

            // We attach the validation errors to the thrown object so components can use them
            return throwError(() => ({
                message: errorMessage,
                status: error.status,
                validationErrors: validationErrors,
                originalError: error
            }));
        })
    );
};
