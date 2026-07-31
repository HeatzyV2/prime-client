/**
 * Static store catalog — Cosmetics Update v2.2 — all Prime cosmetics free.
 */

const STORE_CATALOG = [
  // Capes
  { id: 'cape-prime-classic', name: 'Prime Classic', description: 'Signature blue Prime cape.', price: 0, category: 'cosmetic' },
  { id: 'cape-prime-founder', name: 'Prime Founder', description: 'Founder exclusive cape.', price: 0, category: 'cosmetic' },
  { id: 'cape-prime-neon', name: 'Prime Neon', description: 'Electric neon cape.', price: 0, category: 'cosmetic' },
  { id: 'cape-prime-shadow', name: 'Prime Shadow', description: 'Void shadow cape.', price: 0, category: 'cosmetic' },
  { id: 'cape-prime', name: 'Prime Cape', description: 'Legacy alias of Classic.', price: 0, category: 'cosmetic' },
  { id: 'cape-star', name: 'Star Cape', description: 'Gold star cape for Prime peers.', price: 0, category: 'cosmetic' },
  { id: 'cape-crimson', name: 'Crimson Cape', description: 'Signature crimson cape.', price: 0, category: 'cosmetic' },
  { id: 'cape-midnight', name: 'Midnight Cape', description: 'Indigo midnight cape.', price: 0, category: 'cosmetic' },
  // Wings
  { id: 'wings-inferno', name: 'Inferno Wings', description: 'Blazing inferno wings.', price: 0, category: 'cosmetic' },
  { id: 'wings-shadow', name: 'Shadow Wings', description: 'Dark ethereal wings.', price: 0, category: 'cosmetic' },
  { id: 'wings-galaxy', name: 'Galaxy Wings', description: 'Cosmic galaxy wings.', price: 0, category: 'cosmetic' },
  { id: 'wings-prime', name: 'Prime Wings', description: 'Official Prime wings.', price: 0, category: 'cosmetic' },
  { id: 'wings-aurora', name: 'Aurora Wings', description: 'Animated aurora wings.', price: 0, category: 'cosmetic' },
  { id: 'wings-ember', name: 'Ember Wings', description: 'Animated fiery wings.', price: 0, category: 'cosmetic' },
  // Auras
  { id: 'aura-prime-energy', name: 'Prime Energy', description: 'Blue energy aura.', price: 0, category: 'cosmetic' },
  { id: 'aura-fire', name: 'Fire Aura', description: 'Flame particles.', price: 0, category: 'cosmetic' },
  { id: 'aura-void', name: 'Void Aura', description: 'Dark void swirl.', price: 0, category: 'cosmetic' },
  { id: 'aura-lightning', name: 'Lightning Aura', description: 'Electric sparks.', price: 0, category: 'cosmetic' },
  { id: 'aura-royal', name: 'Royal Aura', description: 'Golden royal glow.', price: 0, category: 'cosmetic' },
  // Trails
  { id: 'trail-flame', name: 'Flame Trail', description: 'Fire trail while moving.', price: 0, category: 'cosmetic' },
  { id: 'trail-star', name: 'Star Trail', description: 'Sparkling star trail.', price: 0, category: 'cosmetic' },
  { id: 'trail-rainbow', name: 'Rainbow Trail', description: 'Rainbow particle trail.', price: 0, category: 'cosmetic' },
  { id: 'trail-shadow', name: 'Shadow Trail', description: 'Dark smoke trail.', price: 0, category: 'cosmetic' },
  { id: 'trail-prime', name: 'Prime Trail', description: 'Prime blue trail.', price: 0, category: 'cosmetic' },
  // Hats
  { id: 'hat-crown', name: 'Prime Crown', description: 'Royal crown hat.', price: 0, category: 'cosmetic' },
  { id: 'hat-horns', name: 'Dragon Horns', description: 'Dragon horn headpiece.', price: 0, category: 'cosmetic' },
  { id: 'hat-wizard', name: 'Wizard Hat', description: 'Classic wizard hat.', price: 0, category: 'cosmetic' },
  { id: 'hat-santa', name: 'Santa Hat', description: 'Festive santa hat.', price: 0, category: 'cosmetic' },
  { id: 'hat-dev', name: 'Developer Cap', description: 'Dev-only style cap.', price: 0, category: 'cosmetic' },
  // Emotes
  { id: 'emote-wave', name: 'Wave', description: 'Friendly wave.', price: 0, category: 'cosmetic' },
  { id: 'emote-dance', name: 'Dance', description: 'Dance loop.', price: 0, category: 'cosmetic' },
  { id: 'emote-sit', name: 'Sit', description: 'Sit pose.', price: 0, category: 'cosmetic' },
  { id: 'emote-laugh', name: 'Laugh', description: 'Laugh animation.', price: 0, category: 'cosmetic' },
  { id: 'emote-cry', name: 'Cry', description: 'Sad cry.', price: 0, category: 'cosmetic' },
  { id: 'emote-flex', name: 'Flex', description: 'Flex muscles.', price: 0, category: 'cosmetic' },
  { id: 'emote-clap', name: 'Clap', description: 'Applause.', price: 0, category: 'cosmetic' },
  { id: 'emote-sleep', name: 'Sleep', description: 'Sleep pose.', price: 0, category: 'cosmetic' },
  { id: 'emote-victory', name: 'Victory', description: 'Victory celebration.', price: 0, category: 'cosmetic' },
  // Themes / backgrounds
  { id: 'theme-crimson', name: 'Crimson Theme', description: 'Signature red Prime theme.', price: 0, category: 'theme' },
  { id: 'theme-midnight', name: 'Midnight Theme', description: 'Cool indigo theme.', price: 0, category: 'theme' },
  { id: 'theme-aurora', name: 'Aurora Theme', description: 'Cyan aurora theme.', price: 0, category: 'theme' },
  { id: 'theme-obsidian', name: 'Obsidian Theme', description: 'Black & champagne gold.', price: 0, category: 'theme' },
  { id: 'theme-ember', name: 'Ember Theme', description: 'Copper glow on charcoal.', price: 0, category: 'theme' },
  { id: 'bg-nebula', name: 'Nebula Background', description: 'Animated space background.', price: 0, category: 'background' },
  // Badges
  { id: 'badge-founder', name: 'Founder Badge', description: 'Early adopter badge.', price: 0, category: 'badge' },
  { id: 'badge-creator', name: 'Creator Badge', description: 'Content creator badge.', price: 0, category: 'badge' },
  { id: 'badge-partner', name: 'Partner Badge', description: 'Partner server badge.', price: 0, category: 'badge' },
  { id: 'badge-supporter', name: 'Supporter Badge', description: 'Community supporter.', price: 0, category: 'badge' },
];

const DEFAULT_OWNED = STORE_CATALOG.map((i) => i.id);

const DEFAULT_EQUIPPED = ['cape-prime-classic', 'wings-prime'];

/** One-time promo codes (normalized uppercase). */
const PROMO_CODES = {
  WELCOME100: { coins: 100, label: 'Welcome bonus' },
  PRIME500: { coins: 500, label: 'Prime starter pack' },
  ELYSIA250: { coins: 250, label: 'Elysia promo' },
  FOUNDER1000: { coins: 1000, label: 'Founder gift' },
};

function getCatalogItem(itemId) {
  return STORE_CATALOG.find((i) => i.id === itemId) || null;
}

function catalogForUser(ownedIds) {
  const owned = new Set(ownedIds || []);
  return STORE_CATALOG.map((item) => ({
    ...item,
    owned: owned.has(item.id),
  }));
}

module.exports = {
  STORE_CATALOG,
  DEFAULT_OWNED,
  DEFAULT_EQUIPPED,
  PROMO_CODES,
  getCatalogItem,
  catalogForUser,
};
