import { Routes } from '@angular/router';
import { authGuard, ownerGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'counter' },
  {
    path: 'login',
    loadComponent: () => import('./core/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'counter',
    canActivate: [authGuard],
    loadComponent: () => import('./counter/counter.component').then((m) => m.CounterComponent),
  },
  {
    path: 'quotes',
    canActivate: [authGuard],
    loadComponent: () => import('./quotes/quotes.component').then((m) => m.QuotesComponent),
  },
  {
    path: 'accounts',
    canActivate: [authGuard],
    loadComponent: () => import('./accounts/accounts.component').then((m) => m.AccountsComponent),
  },
  {
    // Takings and debtors are the owner's business, not the counter's.
    path: 'owner',
    canActivate: [ownerGuard],
    loadComponent: () => import('./owner/owner.component').then((m) => m.OwnerComponent),
  },
  { path: '**', redirectTo: 'counter' },
];
