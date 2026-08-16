#!/usr/bin/env bash
# =============================================================================
# 压测验证脚本（M6：真实负载验证）
# 用法：
#   bash docs/验证/压测.sh --scene order-timeout   # 场景一：超时关单闭环
#   bash docs/验证/压测.sh --scene over-sell       # 场景二：并发超卖防护
#   bash docs/验证/压测.sh --scene rate-limit      # 场景三：网关限流
# 前置：各服务已启动，网关 :9000，order :8080，inventory :8083，auth :8087
# =============================================================================

set -u
GATEWAY=${GATEWAY_URL:-http://localhost:9000}
ORDER_API=${ORDER_API_URL:-http://localhost:8080}
INVENTORY_API=${INVENTORY_API_URL:-http://localhost:8083}
AUTH_API=${AUTH_API_URL:-http://localhost:8087}
SKU=${TEST_SKU:-SKU001}

# 场景一：超时关单闭环（批量下单不审 → 触发关单 → 查状态）
scene_order_timeout() {
  local count=${ORDER_COUNT:-10}
  echo "== 场景一：超时关单闭环（下单 ${count} 单）=="

  # 1. 批量下单（不审单，保持 TO_BE_CONFIRMED）
  for i in $(seq 1 "$count"); do
    curl -s -o /dev/null -w "下单#$i:%{http_code}\n" \
      -X POST "$ORDER_API/api/orders" \
      -H 'Content-Type: application/json' \
      -d "{\"customerId\":\"cust-001\",\"skus\":[{\"skuCode\":\"$SKU\",\"skuName\":\"压测商品\",\"quantity\":1,\"price\":100.00}]}"
  done

  # 2. 触发超时关单（minutes=0：创建时间早于 now 即关闭，模拟 task-service 调度）
  echo "-- 触发超时关单 minutes=0 --"
  curl -s -X POST "$ORDER_API/api/orders/timeout-close?minutes=0"
  echo

  # 3. 统计取消状态订单
  echo "-- 统计 CANCELLED 订单 --"
  local cancelled
  cancelled=$(curl -s "$ORDER_API/api/orders?status=CANCELLED&pageSize=100" | grep -o '"total":[0-9]*' | head -1)
  echo "CANCELLED 订单: $cancelled（预期 >= $count）"
}

# 场景二：并发超卖防护（20 线程 × 6 件 = 需求 120 > 库存 100）
scene_over_sell() {
  local threads=${CONCURRENCY:-20}
  local per_thread=6
  local total=100
  echo "== 场景二：并发超卖防护（${threads} 线程 × ${per_thread} 件，库存 ${total}）=="

  # 1. 重置库存
  curl -s -o /dev/null -X POST "$INVENTORY_API/api/inventory/items" \
    -H 'Content-Type: application/json' \
    -d "{\"skuCode\":\"$SKU\",\"skuName\":\"压测商品\",\"unit\":\"件\",\"totalQuantity\":$total}"

  # 2. 并发预占（后台 20 个进程同时打）
  local success=0
  local tmpdir
  tmpdir=$(mktemp -d)
  for i in $(seq 1 "$threads"); do
    (
      code=$(curl -s -o /dev/null -w '%{http_code}' \
        -X POST "$INVENTORY_API/api/inventory/items/$SKU/reserve?quantity=$per_thread")
      [ "$code" = "200" ] && echo ok > "$tmpdir/$i" || echo fail > "$tmpdir/$i"
    ) &
  done
  wait
  success=$(grep -l '^ok$' "$tmpdir"/* | wc -l)
  rm -rf "$tmpdir"

  # 3. 查最终库存
  echo "-- 成功线程: ${success}（预期 ${success}×${per_thread} ≤ ${total}）--"
  curl -s "$INVENTORY_API/api/inventory/items/$SKU"
  echo
  local reserved
  reserved=$(curl -s "$INVENTORY_API/api/inventory/items/$SKU" | grep -o '"reservedQuantity":[0-9.]*' | head -1)
  echo "最终已预占: $reserved（预期 ≤ $total，不超卖）"
}

# 场景三：网关限流（200 次突刺，burstCapacity=100）
scene_rate_limit() {
  echo "== 场景三：网关限流（200 次突刺，burst=100/s 补充 50）=="

  # 1. 登录拿 Token
  local token
  token=$(curl -s -X POST "$AUTH_API/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"dealer001","password":"123456"}' \
    | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

  # 2. 突刺 200 次打网关 /api/orders
  local ok=0 limited=0 other=0
  for i in $(seq 1 200); do
    code=$(curl -s -o /dev/null -w '%{http_code}' \
      -H "Authorization: Bearer $token" \
      "$GATEWAY/api/orders?pageNum=1&pageSize=1")
    case "$code" in
      200) ok=$((ok+1)) ;;
      429) limited=$((limited+1)) ;;
      *) other=$((other+1)) ;;
    esac
  done

  echo "200 次响应: 2xx=${ok}, 429限流=${limited}, 其他=${other}"
  echo "预期: 前 ~100 次 200，之后 429（burstCapacity=100）"
}

SCENE="${1:-}"
case "$SCENE" in
  --scene order-timeout) scene_order_timeout ;;
  --scene over-sell) scene_over_sell ;;
  --scene rate-limit) scene_rate_limit ;;
  *) echo "用法: $0 --scene {order-timeout|over-sell|rate-limit}"; exit 1 ;;
esac
