export interface PopupAnchor {
  left: number;
  right: number;
  top: number;
  bottom: number;
}

export interface PopupSize {
  width: number;
  height: number;
}

export interface ViewportSize {
  width: number;
  height: number;
}

export interface PopupPositionOptions {
  margin?: number;
  gap?: number;
  preferSide?: boolean;
}

export interface PopupPosition {
  left: number;
  top: number;
}

/**
 * Computes viewport-clamped fixed coordinates for a floating popup relative to [anchor].
 *
 * When `preferSide` is false (default), prefers placing below or above the anchor so the
 * clicked cell stays visible, falling back to beside it.
 * When `preferSide` is true (e.g. a secondary popup beside an open primary popup), prefers
 * placing to the right or left of the anchor first, falling back to below or above.
 */
export function computePopupPosition(
  anchor: PopupAnchor,
  popup: PopupSize,
  viewport: ViewportSize,
  options: PopupPositionOptions = {}
): PopupPosition {
  const margin = options.margin ?? 8;
  const gap = options.gap ?? 4;
  const preferSide = options.preferSide ?? false;
  const {width, height} = popup;
  const {width: vw, height: vh} = viewport;

  const spaceBelow = vh - margin - (anchor.bottom + gap);
  const spaceAbove = anchor.top - gap - margin;
  const spaceRight = vw - margin - (anchor.right + gap);
  const spaceLeft = anchor.left - gap - margin;
  const maxLeft = Math.max(margin, vw - width - margin);
  const maxTop = Math.max(margin, vh - height - margin);

  let left: number;
  let top: number;

  if (preferSide) {
    if (width <= spaceRight || width <= spaceLeft) {
      left = width <= spaceRight ? anchor.right + gap : anchor.left - width - gap;
      top = anchor.top + height <= vh - margin ? anchor.top : anchor.bottom - height;
    } else if (height <= spaceBelow || height <= spaceAbove) {
      top = height <= spaceBelow ? anchor.bottom + gap : anchor.top - height - gap;
      left = anchor.left + width <= vw - margin ? anchor.left : anchor.right - width;
    } else {
      left = spaceLeft > spaceRight ? anchor.left - width - gap : anchor.right + gap;
      top = anchor.top;
    }
  } else {
    if (height <= spaceBelow || height <= spaceAbove) {
      top = height <= spaceBelow ? anchor.bottom + gap : anchor.top - height - gap;
      left = anchor.left + width <= vw - margin ? anchor.left : anchor.right - width;
    } else if (width <= spaceRight || width <= spaceLeft) {
      left = width <= spaceRight ? anchor.right + gap : anchor.left - width - gap;
      top = anchor.bottom - height >= margin ? anchor.bottom - height : anchor.top;
    } else {
      left = spaceLeft > spaceRight ? anchor.right - width : anchor.left;
      top = spaceAbove > spaceBelow ? anchor.top - height - gap : anchor.bottom + gap;
    }
  }

  return {
    left: Math.min(Math.max(margin, left), maxLeft),
    top: Math.min(Math.max(margin, top), maxTop),
  };
}
