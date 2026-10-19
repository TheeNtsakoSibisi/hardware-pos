import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ApiError } from './api.models';

/** Sign-in at the start of a shift. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div style="max-width:420px;margin:12vh auto;padding:0 16px">
      <h1 style="margin-bottom:4px">Sibanye Hardware</h1>
      <p class="muted" style="margin-top:0">Sign in to open the till.</p>

      <div class="panel">
        <label class="f" for="username" style="margin-top:0">Username</label>
        <input id="username" type="text" autocomplete="username" [ngModel]="username()"
               (ngModelChange)="username.set($event)" (keydown.enter)="submit()">

        <label class="f" for="password">Password</label>
        <input id="password" type="password" autocomplete="current-password" [ngModel]="password()"
               (ngModelChange)="password.set($event)" (keydown.enter)="submit()">

        @if (error()) {
          <div class="alert bad" style="margin-top:12px">{{ error() }}</div>
        }

        <button class="btn primary big" style="width:100%;margin-top:16px" [disabled]="busy()" (click)="submit()">
          {{ busy() ? 'Signing in…' : 'Sign in' }}
        </button>

        <p class="muted" style="font-size:13px;margin-bottom:0">
          Demo logins: <b>thandi</b> (cashier), <b>johan</b> (manager), <b>owner</b> (owner).
          Password <b>demo1234</b> for all three.
        </p>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly username = signal('thandi');
  readonly password = signal('');
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  submit() {
    if (!this.username() || !this.password()) {
      this.error.set('Enter a username and password.');
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.username(), this.password()).subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigate(['/counter']);
      },
      error: (err: ApiError) => {
        this.busy.set(false);
        this.error.set(err.message);
      },
    });
  }
}
