import { Component } from '@angular/core';
import { DocumentsComponent } from './documents.component';

@Component({
  selector: 'app-root',
  imports: [DocumentsComponent],
  template: `
    <h1>DocMint — démo</h1>
    <p>PDF générés par l'API de démonstration (http://localhost:8080).</p>
    <app-documents />
  `,
  styles: `:host { font-family: system-ui, sans-serif; display: block; margin: 24px; }`,
})
export class AppComponent {}
