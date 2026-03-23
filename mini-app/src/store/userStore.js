import { create } from 'zustand';

export const useUserStore = create((set) => ({
    // Чистые начальные данные (без "левых" заглушек)
    user: {
        daysLeft: 0,
        trafficUsed: 0,
        active: false,
        currentLocation: 'Не выбрана',
        devices: 0,
        untilDate: null
    },
    loading: false,

    // Метод для обновления данных (вызовешь его, когда будет готов бэкенд)
    setUser: (newData) => set({ user: newData }),

    // Метод для "подключения" (пока просто имитация)
    connectVpn: async () => {
        console.log("Запрос на подключение к GEOVpn...");
        return null;
    }
}));