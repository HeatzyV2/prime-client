import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  getOptionValue,
  mergePresetGameOptions,
  presetGameOptions,
  setOptionValue,
  setOptionValueIfAbsent
} from './optionsMerge'

describe('options.txt merge helpers', () => {
  it('preserves unrelated keys when overwriting a preset field', () => {
    const lines = [
      'maxFps:999',
      'key_key.forward:key.keyboard.w',
      'key_key.jump:key.keyboard.space',
      'fov:90'
    ]
    const next = setOptionValue(lines, 'maxFps', '120')
    assert.equal(getOptionValue(next, 'maxFps'), '120')
    assert.equal(getOptionValue(next, 'key_key.forward'), 'key.keyboard.w')
    assert.equal(getOptionValue(next, 'key_key.jump'), 'key.keyboard.space')
    assert.equal(getOptionValue(next, 'fov'), '90')
  })

  it('fill-absent never clobbers user FPS or keybinds', () => {
    const existing = [
      'maxFps:999',
      'renderDistance:32',
      'key_key.attack:key.mouse.left',
      'graphicsMode:2'
    ]
    const preset = presetGameOptions('balanced', 12)
    const next = mergePresetGameOptions(existing, preset, 'fill-absent')
    assert.equal(getOptionValue(next, 'maxFps'), '999')
    assert.equal(getOptionValue(next, 'renderDistance'), '32')
    assert.equal(getOptionValue(next, 'graphicsMode'), '2')
    assert.equal(getOptionValue(next, 'key_key.attack'), 'key.mouse.left')
    // simulationDistance was missing — seeded once
    assert.equal(getOptionValue(next, 'simulationDistance'), preset.simulationDistance)
  })

  it('overwrite mode applies preset FPS on explicit user apply', () => {
    const existing = ['maxFps:999', 'key_key.forward:key.keyboard.z']
    const preset = presetGameOptions('performance', 16)
    const next = mergePresetGameOptions(existing, preset, 'overwrite')
    assert.equal(getOptionValue(next, 'maxFps'), '240')
    assert.equal(getOptionValue(next, 'renderDistance'), '16')
    assert.equal(getOptionValue(next, 'key_key.forward'), 'key.keyboard.z')
  })

  it('setOptionValueIfAbsent leaves existing values alone', () => {
    const lines = ['maxFps:60']
    const next = setOptionValueIfAbsent(lines, 'maxFps', '120')
    assert.equal(getOptionValue(next, 'maxFps'), '60')
    assert.strictEqual(next, lines)
  })
})
