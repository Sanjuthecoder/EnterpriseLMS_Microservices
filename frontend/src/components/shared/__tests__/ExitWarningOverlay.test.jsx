import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import ExitWarningOverlay from '../ExitWarningOverlay';

// Mock lucide-react icons for simplicity and to avoid SVG parsing issues in Jest node environment
jest.mock('lucide-react', () => ({
  AlertTriangle: () => <div data-testid="alert-triangle" />,
  ArrowLeft: () => <div data-testid="arrow-left" />,
  LogOut: () => <div data-testid="log-out" />,
}));

describe('ExitWarningOverlay Component', () => {
  const mockOnStay = jest.fn();
  const mockOnLeave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders warning text and warning list correctly', () => {
    render(<ExitWarningOverlay onStay={mockOnStay} onLeave={mockOnLeave} />);

    // Assert main header is in the document
    expect(screen.getByText('Assessment In Progress!')).toBeInTheDocument();

    // Assert warnings list items are rendered
    expect(screen.getByText(/Your attempt may be flagged for review/i)).toBeInTheDocument();
    expect(screen.getByText(/Unanswered questions will be marked incorrect/i)).toBeInTheDocument();
    expect(screen.getByText(/Time spent data will be recorded as suspicious/i)).toBeInTheDocument();

    // Assert buttons are present
    expect(screen.getByRole('button', { name: /Return to Quiz/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Leave/i })).toBeInTheDocument();
  });

  test('calls onStay when "Return to Quiz" button is clicked', () => {
    render(<ExitWarningOverlay onStay={mockOnStay} onLeave={mockOnLeave} />);

    const stayButton = screen.getByRole('button', { name: /Return to Quiz/i });
    fireEvent.click(stayButton);

    expect(mockOnStay).toHaveBeenCalledTimes(1);
    expect(mockOnLeave).not.toHaveBeenCalled();
  });

  test('calls onLeave when "Leave" button is clicked', () => {
    render(<ExitWarningOverlay onStay={mockOnStay} onLeave={mockOnLeave} />);

    const leaveButton = screen.getByRole('button', { name: /Leave/i });
    fireEvent.click(leaveButton);

    expect(mockOnLeave).toHaveBeenCalledTimes(1);
    expect(mockOnStay).not.toHaveBeenCalled();
  });
});
