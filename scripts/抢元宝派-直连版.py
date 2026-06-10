#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
元宝派 - 免费 Bot 创建脚本【直连版】
=============================================================================

【功能说明】
    直连抢购元宝派每天 12:00 和 20:00 的免费 Bot 创建名额
    异步协程高并发，提前开始循环，直到抢到成功或超时停止

【青龙定时规则】
    上午场: 58 11 * * *   （11:58 启动，抢 12:00 场）
    下午场: 58 19 * * *   （19:58 启动，抢 20:00 场）

【环境变量】
    变量名: YUANBAO_COOKIE
    变量值: 完整的 Cookie 字符串（多个账号用 & 分隔）

=============================================================================
"""

import os
import time
import asyncio
import aiohttp
from datetime import datetime, timedelta

# 青龙面板推送
try:
    from notify import send
    SEND_FLAG = True
except ImportError:
    SEND_FLAG = False
    def send(title: str, content: str) -> None:
        print(f"[推送] {title}: {content[:200]}")

# ========== 配置 ==========
ADVANCE_SECONDS = 3        # 提前秒数开始高频请求
WARMUP_SECONDS = 60        # 预热阶段提前秒数
MAX_RETRY_SECONDS = 90     # 整点后最多继续抢多少秒
CONCURRENCY = 50           # 并发协程数
REQUEST_TIMEOUT = 5        # 请求超时（秒）

WARMUP_INTERVAL_MS = 500   # 预热间隔
HOT_INTERVAL_MS = 20       # 高频间隔
COOLDOWN_INTERVAL_MS = 50  # 整点后间隔

# ========== Cookie解析 ==========
def parse_cookies():
    """解析多个账号的 Cookie"""
    cookie_str = os.environ.get("YUANBAO_COOKIE", "")
    if not cookie_str:
        print("=" * 50)
        print("❌ 错误: 未设置环境变量 YUANBAO_COOKIE")
        print("=" * 50)
        exit(1)
    
    accounts = []
    for cookie in cookie_str.split("&"):
        cookie = cookie.strip()
        if not cookie:
            continue
        accounts.append({"cookie": cookie})
    
    if not accounts:
        print("❌ 无有效账号")
        exit(1)
    
    return accounts

# ========== 时间工具 ==========
def get_target_time():
    """获取目标场次时间"""
    now = datetime.now()
    if now.hour < 12 or (now.hour == 11 and now.minute >= 55):
        return now.replace(hour=12, minute=0, second=0, microsecond=0), "12:00"
    elif now.hour < 20 or (now.hour == 19 and now.minute >= 55):
        return now.replace(hour=20, minute=0, second=0, microsecond=0), "20:00"
    else:
        tomorrow = now + timedelta(days=1)
        return tomorrow.replace(hour=12, minute=0, second=0, microsecond=0), "明天 12:00"

def precise_sleep(seconds):
    """精确睡眠"""
    if seconds <= 0:
        return
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        remaining = end - time.monotonic()
        if remaining > 0.01:
            time.sleep(remaining * 0.8)

# ========== 核心抢购逻辑 ==========
async def grab_single(session, account, target_time, result_holder):
    """单个协程的抢购逻辑"""
    url = "https://yuanbao.tencent.com/api/v5/robotLogic/create"
    headers = {
        "Host": "yuanbao.tencent.com",
        "Origin": "https://yuanbao.tencent.com",
        "Referer": "https://yuanbao.tencent.com/e/claw/manage",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
        "Content-Type": "application/json",
        "Accept": "application/json, text/plain, */*",
        "Cookie": account["cookie"],
    }
    payload = {"type": 1, "create_type": 1}
    
    request_count = 0
    end_time = target_time + timedelta(seconds=MAX_RETRY_SECONDS)
    
    while not result_holder["success"] and datetime.now() < end_time:
        try:
            request_count += 1
            start = time.monotonic()
            
            async with session.post(
                url,
                json=payload,
                headers=headers,
                timeout=aiohttp.ClientTimeout(total=REQUEST_TIMEOUT),
                ssl=False
            ) as resp:
                cost = int((time.monotonic() - start) * 1000)
                
                if resp.status == 200:
                    data = await resp.json()
                    code = data.get("code", -1)
                    
                    if code == 0:
                        result_holder["success"] = True
                        result_holder["account"] = account["cookie"][:20]
                        result_holder["cost"] = cost
                        result_holder["count"] = request_count
                        
                        print(f"\n{'='*60}")
                        print(f"✅ 抢 Bot 成功！")
                        print(f"   耗时: {cost}ms")
                        print(f"   请求次数: {request_count}")
                        print(f"{'='*60}")
                        return True
                    
                    now = datetime.now()
                    remaining = (target_time - now).total_seconds()
                    if remaining > 0:
                        print(f"[{now.strftime('%H:%M:%S')}] 预热... {remaining:.1f}s", end="\r")
                    else:
                        print(f"[{now.strftime('%H:%M:%S')}] #{request_count} code={code} {cost}ms", end="\r")
                
        except asyncio.TimeoutError:
            pass
        except Exception as e:
            pass
        
        # 动态间隔
        now = datetime.now()
        remaining = (target_time - now).total_seconds()
        
        if remaining > WARMUP_SECONDS:
            await asyncio.sleep(WARMUP_INTERVAL_MS / 1000)
        elif remaining > ADVANCE_SECONDS:
            await asyncio.sleep(WARMUP_INTERVAL_MS / 1000)
        elif remaining > 0:
            await asyncio.sleep(HOT_INTERVAL_MS / 1000)
        else:
            await asyncio.sleep(COOLDOWN_INTERVAL_MS / 1000)
    
    return False

async def grab_batch(accounts, target_time, target_desc):
    """批量并发抢购"""
    result_holder = {
        "success": False,
        "account": None,
        "cost": 0,
        "count": 0,
    }
    
    total_concurrency = CONCURRENCY * len(accounts)
    
    print(f"\n🚀 直连抢购启动！")
    print(f"   目标场次: {target_desc}")
    print(f"   账号数量: {len(accounts)}")
    print(f"   并发协程: {total_concurrency}")
    print(f"   目标时间: {target_time.strftime('%H:%M:%S')}")
    print("-" * 60)
    
    connector = aiohttp.TCPConnector(
        limit=total_concurrency,
        limit_per_host=total_concurrency,
        ttl_dns_cache=300,
    )
    
    async with aiohttp.ClientSession(connector=connector) as session:
        tasks = []
        for account in accounts:
            for _ in range(CONCURRENCY):
                task = asyncio.create_task(
                    grab_single(session, account, target_time, result_holder)
                )
                tasks.append(task)
        
        timeout = MAX_RETRY_SECONDS + max(0, (target_time - datetime.now()).total_seconds()) + 5
        done, pending = await asyncio.wait(
            tasks,
            timeout=timeout,
            return_when=asyncio.FIRST_COMPLETED,
        )
        
        for task in pending:
            task.cancel()
        
        await asyncio.gather(*pending, return_exceptions=True)
    
    return result_holder

# ========== 主函数 ==========
async def main():
    print("\n" + "=" * 60)
    print("元宝派 Bot 抢购脚本【直连版】")
    print(f"当前时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60 + "\n")
    
    accounts = parse_cookies()
    
    print(f"✅ 加载 {len(accounts)} 个账号")
    
    target_time, target_desc = get_target_time()
    now = datetime.now()
    wait_seconds = (target_time - now).total_seconds()
    
    print(f"\n📋 当前状态:")
    print(f"   当前时间: {now.strftime('%H:%M:%S')}")
    print(f"   目标场次: {target_desc}")
    print(f"   距离开始: {wait_seconds/60:.0f} 分钟")
    
    # 等待到预热时间
    warmup_time = target_time - timedelta(seconds=WARMUP_SECONDS)
    now = datetime.now()
    
    if now < warmup_time:
        wait = (warmup_time - now).total_seconds()
        print(f"\n⏰ 等待 {wait:.0f} 秒后进入预热阶段...")
        precise_sleep(wait)
    
    # 开始抢购
    result = await grab_batch(accounts, target_time, target_desc)
    
    # 推送结果
    if result["success"]:
        title = "元宝派 Bot 抢购成功"
        content = (
            f"✅ 抢购成功！\n"
            f"场次: {target_desc}\n"
            f"耗时: {result['cost']}ms\n"
            f"并发数: {CONCURRENCY * len(accounts)}"
        )
    else:
        title = "元宝派 Bot 抢购失败"
        content = (
            f"❌ 未抢到名额\n"
            f"场次: {target_desc}\n"
            f"账号数: {len(accounts)}"
        )
    
    if SEND_FLAG:
        try:
            send(title, content)
            print("\n✅ 推送已发送")
        except Exception as e:
            print(f"\n❌ 推送失败: {e}")
    else:
        print(f"\n{'='*60}")
        print(f"【{title}】")
        print(content)
        print("=" * 60)

if __name__ == "__main__":
    asyncio.run(main())
