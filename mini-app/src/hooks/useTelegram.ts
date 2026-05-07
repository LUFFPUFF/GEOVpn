export function useTelegram() {
    const tg = window.Telegram?.WebApp;

    console.log('[Telegram] WebApp:', tg);
    console.log('[Telegram] initData:', tg?.initData);
    console.log('[Telegram] initDataUnsafe:', tg?.initDataUnsafe);
    console.log('[Telegram] user:', tg?.initDataUnsafe?.user);
    console.log('[Telegram] platform:', tg?.platform);

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
