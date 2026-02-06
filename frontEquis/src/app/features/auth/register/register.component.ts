import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest } from '../../../core/models/auth.model';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './register.component.html',
    styleUrl: './register.component.css'
})
export class RegisterComponent {
    private authService = inject(AuthService);
    private router = inject(Router);
    private toastr = inject(ToastrService);

    data: RegisterRequest = {
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        documentNumber: '',
        documentType: 'DNI',
        phone: ''
    };
    error = '';
    validationErrors: any = {};

    onSubmit() {
        this.authService.register(this.data).subscribe({
            next: () => {
                this.toastr.success('¡Registro exitoso! Ya puedes iniciar sesión', 'Registro');
                this.router.navigate(['/login']);
            },
            error: (err) => {
                this.error = err.message;
                this.toastr.error(this.error || 'Error al registrar usuario', 'Error');
                if (err.validationErrors) {
                    this.validationErrors = err.validationErrors;
                    console.log('Errores de validación recibidos:', this.validationErrors);
                }
            }
        });
    }
}
