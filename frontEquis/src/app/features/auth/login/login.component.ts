import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastrService } from 'ngx-toastr';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent {
    private authService = inject(AuthService);
    private router = inject(Router);
    private toastr = inject(ToastrService);

    email = '';
    password = '';

    onSubmit() {
        if (!this.email || !this.password) return;

        this.authService.login({ email: this.email, password: this.password }).subscribe({
            next: () => {
                this.toastr.success('¡Bienvenido de nuevo!', 'Inicio de Sesión');
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                // El interceptor procesa el error y coloca el mensaje del backend en err.message
                const errorMessage = err.message || 'Error de conexión o credenciales inválidas';
                this.toastr.error(errorMessage, 'Error de Autenticación');
            }
        });
    }
}
