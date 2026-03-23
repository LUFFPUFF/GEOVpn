import WebApp from '@twa-dev/sdk';

export function useTelegram() {
    return {
        tg: WebApp,
        user: WebApp.initDataUnsafe?.user,
        initData: WebApp.initData,
        expand: () => WebApp.expand(),
        close: () => WebApp.close(),
        showAlert: (msg) => WebApp.showAlert(msg),
    };
}