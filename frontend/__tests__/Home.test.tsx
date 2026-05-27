import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Home from '@/app/page';

describe('Home (landing page)', () => {
    it('renders the Hishabi h1 header', () => {
        render(<Home />);
        const heading = screen.getByRole('heading', { name: 'Hishabi', level: 1 });
        expect(heading).toBeDefined();
    });

    it('shows the tagline', () => {
        render(<Home />);
        expect(
            screen.getByText(/Calculators for Bangladeshi finance/i)
        ).toBeDefined();
    });

    it('links to the Income Tax calculator', () => {
        render(<Home />);
        const link = screen.getByRole('link', { name: /Income Tax/i });
        expect(link).toBeDefined();
        expect(link.getAttribute('href')).toBe('/calculators/tax');
    });

    it('shows Zakat as coming soon (no link)', () => {
        render(<Home />);
        expect(screen.getByText('Zakat')).toBeDefined();
        expect(screen.getByText(/Coming soon/i)).toBeDefined();
    });
});