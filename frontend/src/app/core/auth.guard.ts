import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

/** Keeps the counter behind a sign-in, and restores the session after a refresh. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.signedIn()) {
    return true;
  }
  return auth.restore().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};

/** Owner and manager screens: money in, money out. */
export const ownerGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const allow = () => (auth.canSeeOwnerScreens() ? true : router.createUrlTree(['/counter']));

  if (auth.signedIn()) {
    return allow();
  }
  return auth.restore().pipe(
    map(allow),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};
