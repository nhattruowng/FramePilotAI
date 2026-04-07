import type { PropsWithChildren, ReactNode } from "react";

interface ActionCardProps extends PropsWithChildren {
  eyebrow: string;
  title: string;
  action?: ReactNode;
}

export function ActionCard({ eyebrow, title, action, children }: ActionCardProps) {
  return (
    <section className="card">
      <div className="card-header">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
        </div>
        {action}
      </div>
      <div className="card-body">{children}</div>
    </section>
  );
}
