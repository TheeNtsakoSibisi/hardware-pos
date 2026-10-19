import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { ApiService } from './api.service';
import { Role, Session } from './api.models';

/** Who is on the till right now. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly session = signal<Session | null>(null);
  readonly signedIn = computed(() => this.session() !== null);
  readonly canSeeOwnerScreens = computed(() => this.hasRole('MANAGER', 'OWNER'));

  login(username: string, password: string) {
    return this.api.login(username, password).pipe(tap((session) => this.session.set(session)));
  }

  restore() {
    return this.api.session().pipe(tap((session) => this.session.set(session)));
  }

  logout() {
    this.api.logout().subscribe({
      next: () => this.afterLogout(),
      error: () => this.afterLogout(),
    });
  }

  hasRole(...roles: Role[]): boolean {
    const current = this.session();
    return current !== null && roles.includes(current.role);
  }

  private afterLogout() {
    this.session.set(null);
    void this.router.navigate(['/login']);
  }
}
