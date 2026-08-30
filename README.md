## About
> Sorry: Not translated into English yet.

自`CraftUs`项目的重置

Mod信息:
- 当前版本为: `1.20.1-Fabric`
- `ChaosRig`需要双端装载, 要求同时装载`ChaosRigApi`作为依赖
- `ChaosRigApi`客户端可独立装载
> 好笑的是`ChaosRigApi`并没有干api的事情, 实际上只是个方便的工具并兼顾了仅客户端的情况

⚠ 该MOD开发中, 包含许多未测试内容

## 修正内容
- 在标记在同步时爆空指针的问题, 已插入判断防止
- 修复了在被标记时，自己可见该标记
- 修复了客户端标记方块实体仍然为位置型标记
- 取消了重复标记客户端不会重置渲染进度
- 添加"集合"(`Regroup`)标记，并分清了长按和短按的功能
- 补充了language文件
- 添加团结之力（测试机制）
- 补充`ResourceHelper`功能，`TooltipReader`不再频繁读取文件, 而是读取运存中保存的值
- `InformationScreen`不再会在非测试环境注册运行
- 为`OutlineRenderer`回归了一项功能（渲染实体）
- 修复判断条件与警告提示不符内容
- 删除了一些不必要的内容
> 表述`回归`指：将`CraftUs`的代码重新添加回本项目

本次commit修正内容：
- 修复读取配置文件时，始终读取默认值，无法读取文件存储的值

## 配置文件相应功能
`chaos_rig_client.json`

```json5

```
> 该文件目前确实啥也没有

`chaos_rig_server.json`
```json5
{
  /* 标记功能 */
  "ping": {
    "location_alive_max_tick": 1200, // 位置型标记存在时间(tick, 1200t=60s)
    "entity_alive_default_max_tick": 200, // 实体型标记存在时间(tick, 200t=10s)
    "block_alive_max_tick": 900, // 方块实体型标记存在时间(tick, 900t=45s)
    "regroup_alive_max_tick": 1200, // 集合标记存在时间
    "max_distance": 128 // 一名实体最大标记距离
  },
  /* 游戏机制 */
  "func": {
    /* 团结之力 */
    "stay_together": {
      "enable": false, // 启用
      "max_distance": 12, // 最大容忍距离
      "damage": 1, // 一秒一次的伤害
      "damaging_delay": 5 // 超出距离后的伤害延迟
    }
  }
}
```
> Note: 注释在实际文件并不存在, 旨在解释功能