import { DiscordIpcClient } from '../src/main/discord/DiscordIpcClient.ts'

const client = new DiscordIpcClient('1525574680994648174')

const ok = await client.setActivity({
  details: 'Prime Launcher',
  state: 'Test depuis TS',
  largeImageKey: 'prime_logo',
  largeImageText: 'Prime',
  buttons: [
    { label: 'Prime Client', url: 'https://discord.com/applications/1525574680994648174' },
    { label: 'Discord', url: 'https://discord.com/app' }
  ]
})

console.log({ ok, connected: client.isConnected(), lastError: client.lastError })
