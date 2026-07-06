import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AppLifecycleService implements OnDestroy {
  private readonly refreshSubject = new Subject<void>();
  private readonly onlineSubject = new BehaviorSubject<boolean>(navigator.onLine);

  readonly refresh$ = this.refreshSubject.asObservable();
  readonly online$ = this.onlineSubject.asObservable();

  private readonly onVisibilityChange = () => {
    if (document.visibilityState === 'visible' && navigator.onLine) {
      this.refreshSubject.next();
    }
  };

  private readonly onOnline = () => {
    this.onlineSubject.next(true);
    this.refreshSubject.next();
  };

  private readonly onOffline = () => this.onlineSubject.next(false);

  constructor() {
    document.addEventListener('visibilitychange', this.onVisibilityChange);
    window.addEventListener('online', this.onOnline);
    window.addEventListener('offline', this.onOffline);
  }

  ngOnDestroy() {
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    window.removeEventListener('online', this.onOnline);
    window.removeEventListener('offline', this.onOffline);
  }
}
