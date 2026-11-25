import {expect} from '@esm-bundle/chai';
import {computePopupPosition} from './popup-position';

describe('computePopupPosition', () => {
  it('places popup below and left-aligned when space permits', () => {
    const pos = computePopupPosition(
      {left: 20, right: 80, top: 40, bottom: 60},
      {width: 300, height: 200},
      {width: 1200, height: 800}
    );
    expect(pos).to.deep.equal({left: 20, top: 64});
  });

  it('flips popup above and leftward near bottom-right corner', () => {
    const pos = computePopupPosition(
      {left: 1120, right: 1190, top: 760, bottom: 784},
      {width: 300, height: 200},
      {width: 1200, height: 800}
    );
    expect(pos).to.deep.equal({left: 890, top: 556});
  });

  it('places beside anchor on short viewports when neither below nor above fits', () => {
    const posRight = computePopupPosition(
      {left: 100, right: 180, top: 140, bottom: 164},
      {width: 300, height: 250},
      {width: 1200, height: 320}
    );
    expect(posRight.left).to.equal(184);
    expect(posRight.top).to.be.at.least(8);

    const posLeft = computePopupPosition(
      {left: 950, right: 1020, top: 140, bottom: 164},
      {width: 300, height: 250},
      {width: 1200, height: 320}
    );
    expect(posLeft.left).to.equal(646);
  });

  it('clamps inside viewport when popup exceeds all available space', () => {
    const pos = computePopupPosition(
      {left: 200, right: 250, top: 150, bottom: 170},
      {width: 900, height: 600},
      {width: 500, height: 400}
    );
    expect(pos).to.deep.equal({left: 8, top: 8});
  });

  it('prefers placing to the right or left when preferSide is true', () => {
    const posRight = computePopupPosition(
      {left: 50, right: 550, top: 100, bottom: 120},
      {width: 400, height: 260},
      {width: 1200, height: 800},
      {preferSide: true}
    );
    expect(posRight).to.deep.equal({left: 554, top: 100});

    const posLeft = computePopupPosition(
      {left: 650, right: 1150, top: 100, bottom: 120},
      {width: 400, height: 260},
      {width: 1200, height: 800},
      {preferSide: true}
    );
    expect(posLeft).to.deep.equal({left: 246, top: 100});

    // Falls back to below/above when neither side fits horizontally
    const posBelow = computePopupPosition(
      {left: 200, right: 700, top: 60, bottom: 120},
      {width: 450, height: 200},
      {width: 800, height: 700},
      {preferSide: true}
    );
    expect(posBelow.top).to.equal(124);

    // Clamps when neither side nor vertical fits
    const posClamped = computePopupPosition(
      {left: 100, right: 400, top: 120, bottom: 200},
      {width: 500, height: 500},
      {width: 480, height: 300},
      {preferSide: true}
    );
    expect(posClamped).to.deep.equal({left: 8, top: 8});
  });
});
