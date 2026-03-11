import asyncio
import io
import os
import qrcode
import hmac
import hashlib
import json
from urllib.parse import parse_qsl
from dotenv import load_dotenv

from aiogram import Bot, Dispatcher, F, Router
from aiogram.filters import Command, CommandStart
from aiogram.types import (
    Message,
    CallbackQuery,
    InlineKeyboardMarkup,
    InlineKeyboardButton,
    WebAppInfo,
    BufferedInputFile
)
from aiogram.client.session.aiohttp import AiohttpSession
from aiogram.client.telegram import TelegramAPIServer  # Для обхода блокировок

load_dotenv()
BOT_TOKEN = os.getenv("BOT_TOKEN")

if not BOT_TOKEN:
    raise ValueError("Токен бота не найден! Проверь файл .env")

router = Router()

MOCK_VLESS = "vless://mock-uuid-1234-5678-90ab@8.8.8.8:443?type=tcp&security=reality&pbk=fake-key&sni=yahoo.com#GEOVpn-Smart"

# ВАЖНО: Используй здесь ссылку, которую тебе выдал SSH-туннель (pinggy или serveo)
# Она ОБЯЗАТЕЛЬНО должна начинаться на HTTPS, иначе Telegram выдаст ошибку Bad Request
MINI_APP_URL = "https://твоя-ссылка-из-туннеля.serveo.net"


def get_main_keyboard() -> InlineKeyboardMarkup:
    return InlineKeyboardMarkup(inline_keyboard=[
        [
            # Теперь используем полноценный WebAppInfo
            InlineKeyboardButton(
                text="📱 Открыть GEOVpn",
                web_app=WebAppInfo(url=MINI_APP_URL)
            )
        ],
        [
            InlineKeyboardButton(text="🔑 Получить конфиг", callback_data="get_config"),
            InlineKeyboardButton(text="👤 Профиль", callback_data="show_profile")
        ],
        [
            InlineKeyboardButton(text="❓ Помощь", callback_data="show_help")
        ]
    ])


@router.message(CommandStart())
async def cmd_start(message: Message):
    welcome_text = (
        f"👋 Добро пожаловать, <b>{message.from_user.first_name}</b>!\n\n"
        f"Я — <b>GEOVpn Bot</b>. Твой доступ к безопасному интернету готов.\n\n"
        f"👇 Используй кнопку ниже для запуска приложения:"
    )
    await message.answer(welcome_text, reply_markup=get_main_keyboard(), parse_mode="HTML")


@router.callback_query(F.data == "get_config")
async def cb_get_config(callback: CallbackQuery):
    qr = qrcode.QRCode(version=1, box_size=10, border=4)
    qr.add_data(MOCK_VLESS)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    bio = io.BytesIO()
    img.save(bio, format='PNG')
    bio.seek(0)
    photo = BufferedInputFile(bio.getvalue(), filename="vless_qr.png")

    await callback.message.answer_photo(
        photo=photo,
        caption=f"✅ <b>Ваш ключ готов:</b>\n\n<code>{MOCK_VLESS}</code>",
        parse_mode="HTML"
    )
    await callback.answer()


@router.callback_query(F.data == "show_profile")
async def cb_show_profile(callback: CallbackQuery):
    await callback.message.answer(
        f"👤 <b>Профиль:</b>\n🆔 ID: <code>{callback.from_user.id}</code>\n💰 Баланс: 0.00 ₽",
        parse_mode="HTML"
    )
    await callback.answer()


@router.callback_query(F.data == "show_help")
async def cb_show_help(callback: CallbackQuery):
    await callback.message.answer("По всем вопросам: @support_geovpn")
    await callback.answer()


async def main():
    # --- СЕКРЕТ SENIOR-РАЗРАБОТКИ ДЛЯ ОБХОДА БЛОКИРОВОК ---
    # Мы перенаправляем запросы на зеркало API Telegram, которое не блокируется.
    session = AiohttpSession(
        api=TelegramAPIServer.from_base("https://api.telegram-proxy.org")
    )

    bot = Bot(token=BOT_TOKEN, session=session)
    dp = Dispatcher()
    dp.include_router(router)

    print("--- GEOVpn успешно запущен через зеркало API ---")

    await bot.delete_webhook(drop_pending_updates=True)
    try:
        await dp.start_polling(bot)
    finally:
        await session.close()


if __name__ == "__main__":
    asyncio.run(main())