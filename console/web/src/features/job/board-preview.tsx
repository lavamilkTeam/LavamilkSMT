import type { Placement } from "./model";

export function BoardPreview({
  rows,
  selected,
  select,
  completed,
}: {
  rows: Placement[];
  selected: string | null;
  select: (id: string) => void;
  completed: string[];
}) {
  const xs = rows.map((p) => p.x),
    ys = rows.map((p) => p.y);
  const minX = Math.min(0, ...xs) - 5,
    minY = Math.min(0, ...ys) - 5;
  const width = Math.max(70, ...xs) - minX + 5,
    height = Math.max(55, ...ys) - minY + 5;
  return (
    <svg
      className="board-preview"
      viewBox={`${minX - 8} ${minY - 8} ${width + 16} ${height + 16}`}
      role="img"
      aria-label="当前元件坐标分布示意图，可点击选择元件"
    >
      <rect
        x={minX}
        y={minY}
        width={width}
        height={height}
        rx="2"
        fill="#eff7f5"
        stroke="#8fb9af"
        strokeWidth=".5"
      />
      <path
        d={`M ${minX + 2} 0 H ${minX + 12} M 0 ${minY + 2} V ${minY + 12}`}
        stroke="#65988d"
        strokeWidth=".5"
      />
      <text x={minX + 2} y={minY + 6} fontSize="3" fill="#49766b">
        PCB · 示意
      </text>
      {rows.map((p) => (
        <g
          key={p.id}
          role="button"
          tabIndex={0}
          aria-label={`选择元件 ${p.id}`}
          onClick={() => select(p.id)}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              select(p.id);
            }
          }}
          className="board-part"
          opacity={p.enabled ? 1 : 0.3}
        >
          <circle cx={p.x} cy={p.y} r="4" fill="transparent" />
          {selected === p.id && (
            <circle
              cx={p.x}
              cy={p.y}
              r="4"
              fill="#d6e9ff"
              stroke="#3c7bc9"
              strokeWidth=".5"
            />
          )}
          <rect
            x={p.x - 1.5}
            y={p.y - 0.8}
            width="3"
            height="1.6"
            rx=".25"
            transform={`rotate(${p.rotation} ${p.x} ${p.y})`}
            fill={completed.includes(p.id) ? "#127769" : "#637a87"}
          />
          <title>
            {p.id} · {p.part} · {p.x}, {p.y} mm
          </title>
        </g>
      ))}
    </svg>
  );
}
