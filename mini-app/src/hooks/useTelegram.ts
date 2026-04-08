export function useTelegram() {
    const tg = window.Telegram?.WebApp;

    const expand = () => {
        tg?.expand();
    };

    return {
        tg,
        expand,
        user: tg?.initDataUnsafe?.user,
        queryId: tg?.initDataUnsafe?.query_id,
    };
}