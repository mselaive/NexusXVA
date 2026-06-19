"use client";

import React, { useState } from "react";
import { Info } from "lucide-react";

type InfoButtonProps = {
  title: string;
  body: string;
};

export function InfoButton({ title, body }: InfoButtonProps) {
  const [open, setOpen] = useState(false);

  return (
    <span className="info-wrap">
      <button
        aria-label={title}
        className="icon-btn"
        title={title}
        type="button"
        onClick={() => setOpen((current) => !current)}
      >
        <Info size={16} />
      </button>
      {open ? (
        <span className="info-popover" role="status">
          <strong>{title}</strong>
          <span>{body}</span>
        </span>
      ) : null}
    </span>
  );
}
