import 'package:flutter/material.dart';
import '../theme/miuix_theme.dart';

class MiuixCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? onTap;
  final Color? color;

  const MiuixCard({
    super.key,
    required this.child,
    this.padding,
    this.onTap,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final cardColor = color ?? (isDark ? MiuixColors.darkSurfaceContainer : MiuixColors.surfaceContainer);
    final borderColor = isDark ? MiuixColors.darkOutline : MiuixColors.outline;

    return Container(
      decoration: BoxDecoration(
        color: cardColor,
        borderRadius: BorderRadius.circular(MiuixSpacing.cardCornerRadius),
        border: Border.all(color: borderColor.withValues(alpha: 0.3), width: 0.5),
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(MiuixSpacing.cardCornerRadius),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(MiuixSpacing.cardCornerRadius),
          child: Padding(
            padding: padding ?? const EdgeInsets.all(12),
            child: child,
          ),
        ),
      ),
    );
  }
}

class MiuixSectionHeader extends StatelessWidget {
  final String title;
  final Widget? trailing;

  const MiuixSectionHeader({
    super.key,
    required this.title,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Row(
        children: [
          Text(
            title,
            style: MiuixTextStyles.headline2.copyWith(
              fontWeight: FontWeight.w600,
              color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
            ),
          ),
          if (trailing != null) ...[
            const Spacer(),
            trailing!,
          ],
        ],
      ),
    );
  }
}

class MiuixStatusBadge extends StatelessWidget {
  final String text;
  final Color color;
  final Color? textColor;

  const MiuixStatusBadge({
    super.key,
    required this.text,
    required this.color,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: textColor ?? color,
        ),
      ),
    );
  }
}

class MiuixEmptyState extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final Widget? action;

  const MiuixEmptyState({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
    this.action,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 64,
              color: isDark
                  ? MiuixColors.darkOnSurfaceContainerVariant
                  : MiuixColors.onSurfaceContainerVariant,
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: MiuixTextStyles.headline2.copyWith(
                color: isDark
                    ? MiuixColors.darkOnSurfaceVariantSummary
                    : MiuixColors.onSurfaceVariantSummary,
              ),
              textAlign: TextAlign.center,
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 8),
              Text(
                subtitle!,
                style: MiuixTextStyles.body2.copyWith(
                  color: isDark
                      ? MiuixColors.darkOnSurfaceVariantActions
                      : MiuixColors.onSurfaceVariantActions,
                ),
                textAlign: TextAlign.center,
              ),
            ],
            if (action != null) ...[
              const SizedBox(height: 24),
              action!,
            ],
          ],
        ),
      ),
    );
  }
}

class MiuixLoadingState extends StatelessWidget {
  final String? message;

  const MiuixLoadingState({super.key, this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const CircularProgressIndicator(),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(
              message!,
              style: MiuixTextStyles.body2.copyWith(
                color: MiuixColors.onSurfaceVariantSummary,
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class MiuixErrorState extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;

  const MiuixErrorState({
    super.key,
    required this.message,
    this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.error_outline,
              size: 64,
              color: MiuixColors.error,
            ),
            const SizedBox(height: 16),
            Text(
              message,
              style: MiuixTextStyles.body1.copyWith(
                color: MiuixColors.error,
              ),
              textAlign: TextAlign.center,
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh),
                label: const Text('重试'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class MiuixInfoRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final VoidCallback? onTap;

  const MiuixInfoRow({
    super.key,
    required this.icon,
    required this.label,
    required this.value,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
        child: Row(
          children: [
            Icon(
              icon,
              size: 18,
              color: isDark
                  ? MiuixColors.darkOnSurfaceVariantSummary
                  : MiuixColors.onSurfaceVariantSummary,
            ),
            const SizedBox(width: 10),
            Text(
              label,
              style: MiuixTextStyles.body2.copyWith(
                color: isDark
                    ? MiuixColors.darkOnSurfaceVariantSummary
                    : MiuixColors.onSurfaceVariantSummary,
              ),
            ),
            const Spacer(),
            Flexible(
              child: Text(
                value,
                style: MiuixTextStyles.body2.copyWith(
                  fontWeight: FontWeight.w500,
                  color: isDark ? MiuixColors.darkOnSurface : MiuixColors.onSurface,
                ),
                textAlign: TextAlign.right,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
