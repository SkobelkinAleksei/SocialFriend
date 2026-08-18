import React from 'react';
import ReactDOM from 'react-dom/client';
import App from '@/App';
import '@/styles/global.css';
import { registerServiceWorker } from '@/shared/lib/pwaInstall';
import { listenForInstallPrompt } from '@/shared/lib/pwa';

listenForInstallPrompt();
registerServiceWorker();

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App/>
    </React.StrictMode>
);